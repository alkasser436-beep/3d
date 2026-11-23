// Scene Controller, Orbit/Pinch Camera, Enclosed Interior Camera, 3D Raycasting & Android Bridge
(function (global) {
  const canvas = document.getElementById('glCanvas');
  const renderer = global.RendererLib.createRenderer(canvas);
  const gl = renderer ? renderer.gl : null;

  let currentDesign = null;
  let selectedTarget = { type: 'NONE', id: '' };
  let roomGpuMeshes = [];
  let furnitureCache = new Map();

  const cam = {
    azimuth: 0.72,
    elevation: 0.54,
    distance: 6.4,
    target: [0, 0.9, 0],
    fov: 48 * Math.PI / 180
  };
  let animCam = null;

  function resizeCanvas(overrideW, overrideH) {
    const dpr = Math.min(window.devicePixelRatio || 1, 2.5);
    const w = overrideW || Math.max(2, Math.floor(canvas.clientWidth * dpr));
    const h = overrideH || Math.max(2, Math.floor(canvas.clientHeight * dpr));
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width = w;
      canvas.height = h;
    }
  }

  function getCameraEye(c) {
    const cosE = Math.cos(c.elevation);
    const sinE = Math.sin(c.elevation);
    const cosA = Math.cos(c.azimuth);
    const sinA = Math.sin(c.azimuth);
    return [
      c.target[0] + c.distance * cosE * sinA,
      c.target[1] + c.distance * sinE,
      c.target[2] + c.distance * cosE * cosA
    ];
  }

  function rebuildRoomMeshes() {
    if (!renderer || !currentDesign) return;
    for (const m of roomGpuMeshes) renderer.deleteBufferMesh(m.gpuMesh);
    roomGpuMeshes = [];

    const W = currentDesign.widthMeters;
    const L = currentDesign.lengthMeters;
    const H = currentDesign.wallHeightMeters;
    const wallT = 0.14;
    const ops = currentDesign.openings || [];

    // Floor
    const floorB = global.GeometryLib.createMeshBuilder();
    floorB.addBox(0, -0.04, 0, W + wallT * 2, 0.08, L + wallT * 2, 0.45);
    roomGpuMeshes.push({
      tag: 'FLOOR',
      gpuMesh: renderer.createBufferMesh(floorB.build()),
      modelMatrix: global.Mat4.identity(),
      castShadow: false,
      isFloor: true
    });

    // Ceiling + recessed architectural ceiling downlights
    const ceilB = global.GeometryLib.createMeshBuilder();
    ceilB.addBox(0, H + 0.04, 0, W + wallT * 2, 0.08, L + wallT * 2, 0.35);
    roomGpuMeshes.push({
      tag: 'CEILING',
      gpuMesh: renderer.createBufferMesh(ceilB.build()),
      modelMatrix: global.Mat4.identity(),
      castShadow: false
    });

    const spotRingB = global.GeometryLib.createMeshBuilder();
    const spotBulbB = global.GeometryLib.createMeshBuilder();
    const sx = W * 0.24;
    const sz = L * 0.24;
    [[-sx, -sz], [sx, -sz], [-sx, sz], [sx, sz]].forEach(([px, pz]) => {
      spotRingB.addCylinder(px, H - 0.004, pz, 0.09, 0.015, 16);
      spotBulbB.addCylinder(px, H - 0.01, pz, 0.065, 0.012, 16);
    });
    roomGpuMeshes.push({
      tag: 'CEILING_SPOT_RING',
      gpuMesh: renderer.createBufferMesh(spotRingB.build()),
      modelMatrix: global.Mat4.identity(),
      castShadow: false
    });
    roomGpuMeshes.push({
      tag: 'CEILING_SPOT_BULB',
      gpuMesh: renderer.createBufferMesh(spotBulbB.build()),
      modelMatrix: global.Mat4.identity(),
      emissive: 0.94,
      castShadow: false
    });

    // 4 Walls with cutouts + baseboards + crown molding
    const walls = [
      { id: 'NORTH', span: W, tx: 0, tz: -L * 0.5 - wallT * 0.5, rot: 0, normal: [0, 0, 1] },
      { id: 'SOUTH', span: W, tx: 0, tz: L * 0.5 + wallT * 0.5, rot: Math.PI, normal: [0, 0, -1] },
      { id: 'WEST', span: L, tx: -W * 0.5 - wallT * 0.5, tz: 0, rot: Math.PI * 0.5, normal: [1, 0, 0] },
      { id: 'EAST', span: L, tx: W * 0.5 + wallT * 0.5, tz: 0, rot: -Math.PI * 0.5, normal: [-1, 0, 0] }
    ];

    for (const wInfo of walls) {
      const wallOps = ops.filter(o => o.wall === wInfo.id);
      const wallMesh = global.GeometryLib.buildWallWithOpenings(wInfo.span + wallT * 2, H, wallT, wallOps);
      const model = global.Mat4.multiply(
        global.Mat4.translate(wInfo.tx, 0, wInfo.tz),
        global.Mat4.rotateY(wInfo.rot)
      );
      roomGpuMeshes.push({
        tag: 'WALL',
        wallId: wInfo.id,
        wallNormal: wInfo.normal,
        wallCenter: [wInfo.tx, H * 0.5, wInfo.tz],
        gpuMesh: renderer.createBufferMesh(wallMesh),
        modelMatrix: model,
        castShadow: true
      });

      // Baseboard & crown molding trim
      const trimB = global.GeometryLib.createMeshBuilder();
      trimB.addBox(0, 0.048, wallT * 0.5 + 0.009, wInfo.span, 0.096, 0.018, 0.8);
      trimB.addBox(0, H - 0.035, wallT * 0.5 + 0.012, wInfo.span, 0.07, 0.024, 0.8);
      roomGpuMeshes.push({
        tag: 'BASEBOARD',
        wallId: wInfo.id,
        wallNormal: wInfo.normal,
        wallCenter: [wInfo.tx, 0.05, wInfo.tz],
        gpuMesh: renderer.createBufferMesh(trimB.build()),
        modelMatrix: model,
        castShadow: false
      });

      for (const op of wallOps) {
        const opParts = global.GeometryLib.buildOpeningParts(op, H, wallT);
        const clampedOffset = Math.max(-wInfo.span * 0.5 + op.widthMeters * 0.5 + 0.1,
          Math.min(wInfo.span * 0.5 - op.widthMeters * 0.5 - 0.1, op.offsetMeters));
        const opLocal = global.Mat4.translate(clampedOffset, 0, 0);
        const opModel = global.Mat4.multiply(model, opLocal);
        for (const p of opParts) {
          roomGpuMeshes.push({
            tag: 'OPENING',
            role: p.role,
            wallId: wInfo.id,
            wallNormal: wInfo.normal,
            wallCenter: [wInfo.tx, H * 0.5, wInfo.tz],
            gpuMesh: renderer.createBufferMesh(p.mesh),
            modelMatrix: opModel,
            emissive: p.emissive,
            opacity: p.role === 'window_glass' ? 0.52 : 1.0,
            castShadow: p.role !== 'window_glass'
          });
        }
      }
    }
  }

  function syncFurnitureMeshes() {
    if (!renderer || !currentDesign) return;
    const items = currentDesign.furniture || [];
    const activeIds = new Set(items.map(i => i.id));

    for (const [id, entry] of furnitureCache.entries()) {
      if (!activeIds.has(id)) {
        for (const p of entry.parts) renderer.deleteBufferMesh(p.gpuMesh);
        furnitureCache.delete(id);
      }
    }

    for (const item of items) {
      const sig = `${item.kind}_${item.widthMeters.toFixed(3)}_${item.depthMeters.toFixed(3)}_${item.heightMeters.toFixed(3)}`;
      const existing = furnitureCache.get(item.id);
      if (!existing || existing.sig !== sig) {
        if (existing) {
          for (const p of existing.parts) renderer.deleteBufferMesh(p.gpuMesh);
        }
        const builtParts = global.GeometryLib.buildFurnitureParts(item).map(p => ({
          gpuMesh: renderer.createBufferMesh(p.mesh),
          role: p.role,
          emissive: p.emissive
        }));
        furnitureCache.set(item.id, { sig, parts: builtParts });
      }
    }
  }

  function resolveMaterialEntry(spec) {
    return global.MaterialsLib.getOrCreateTexture(gl, spec.finish, spec.hexColor);
  }

  function buildRenderFrameData(eye) {
    const renderList = [];
    const pointLights = [];
    const W = currentDesign.widthMeters;
    const L = currentDesign.lengthMeters;
    const H = currentDesign.wallHeightMeters;

    const wallMat = resolveMaterialEntry(currentDesign.wallMaterial);
    const floorMat = resolveMaterialEntry(currentDesign.floorMaterial);
    const ceilMat = resolveMaterialEntry(currentDesign.ceilingMaterial);
    const counterMat = resolveMaterialEntry(currentDesign.countertopMaterial);
    const frameMat = global.MaterialsLib.getOrCreateTexture(gl, 'MATTE_PAINT', '#F5F3EE');
    const doorWoodMat = global.MaterialsLib.getOrCreateTexture(gl, 'WOOD_OAK', '#C8A57B');
    const glassMat = global.MaterialsLib.getOrCreateTexture(gl, 'GLOSSY_PAINT', '#C6E2F5');
    const metalMat = global.MaterialsLib.getOrCreateTexture(gl, 'BRUSHED_METAL', '#C8CDD4');
    const fabricAltMat = global.MaterialsLib.getOrCreateTexture(gl, 'FABRIC_WOVEN', '#F4F1EA');
    const darkScreenMat = global.MaterialsLib.getOrCreateTexture(gl, 'GLOSSY_PAINT', '#15181C');
    const lampMat = global.MaterialsLib.getOrCreateTexture(gl, 'FABRIC_WOVEN', '#FFF1D4');
    const woodLegsMat = global.MaterialsLib.getOrCreateTexture(gl, 'WOOD_WALNUT', '#6E4C36');
    const outdoorMat = global.MaterialsLib.getOrCreateTexture(gl, 'OUTDOOR_SCENERY', '#DCE5EC');
    const kitchenTileMat = global.MaterialsLib.getOrCreateTexture(
      gl,
      'CERAMIC_TILE',
      currentDesign.wallMaterial ? currentDesign.wallMaterial.hexColor : '#EFECE5'
    );

    const isTopView = cam.elevation > 1.25;
    const isInsideRoom = Math.abs(eye[0]) < W * 0.49 && Math.abs(eye[2]) < L * 0.49 && eye[1] < H - 0.04;

    for (const m of roomGpuMeshes) {
      if (m.tag === 'CEILING' || m.tag === 'CEILING_SPOT_RING' || m.tag === 'CEILING_SPOT_BULB') {
        if (!isInsideRoom && (isTopView || eye[1] > H - 0.05)) continue;
        let matEntry = ceilMat;
        if (m.tag === 'CEILING_SPOT_RING') matEntry = metalMat;
        if (m.tag === 'CEILING_SPOT_BULB') matEntry = lampMat;
        renderList.push({
          gpuMesh: m.gpuMesh,
          modelMatrix: m.modelMatrix,
          matEntry,
          opacity: 1.0,
          emissive: m.emissive || 0.0,
          castShadow: false,
          selected: m.tag === 'CEILING' && selectedTarget.type === 'CEILING'
        });
      } else if (m.tag === 'FLOOR') {
        renderList.push({
          gpuMesh: m.gpuMesh,
          modelMatrix: m.modelMatrix,
          matEntry: floorMat,
          opacity: 1.0,
          emissive: 0.0,
          castShadow: false,
          isFloor: true,
          selected: selectedTarget.type === 'FLOOR'
        });
      } else {
        if (m.wallNormal && !isTopView && !isInsideRoom) {
          const toCamX = eye[0] - m.wallCenter[0];
          const toCamZ = eye[2] - m.wallCenter[2];
          const dot = toCamX * m.wallNormal[0] + toCamZ * m.wallNormal[2];
          if (dot < -0.15) continue;
        }

        let matEntry = wallMat;
        let selected = selectedTarget.type === 'WALLS';
        if (m.tag === 'WALL' && currentDesign.roomType === 'KITCHEN' &&
            (m.wallId === 'NORTH' || m.wallId === 'WEST') &&
            (currentDesign.wallMaterial.finish === 'MATTE_PAINT' || currentDesign.wallMaterial.finish === 'CERAMIC_TILE')) {
          matEntry = kitchenTileMat;
        }
        if (m.tag === 'BASEBOARD') {
          matEntry = frameMat;
          selected = false;
        } else if (m.tag === 'OPENING') {
          selected = false;
          if (m.role === 'window_frame' || m.role === 'door_frame') matEntry = frameMat;
          else if (m.role === 'door_leaf') matEntry = doorWoodMat;
          else if (m.role === 'window_glass') matEntry = glassMat;
          else if (m.role === 'outdoor_backdrop') matEntry = outdoorMat;
          else if (m.role === 'metal') matEntry = metalMat;
        }

        renderList.push({
          gpuMesh: m.gpuMesh,
          modelMatrix: m.modelMatrix,
          matEntry,
          opacity: m.opacity || 1.0,
          emissive: m.emissive || 0.0,
          castShadow: m.castShadow,
          selected
        });
      }
    }

    if (currentDesign.realisticView) {
      const spotIntensity = currentDesign.lightingPreset === 'INDOOR_SPOTLIGHTS' ? 2.6 : 0.85;
      pointLights.push([-W * 0.24, H - 0.16, -L * 0.24, spotIntensity]);
      pointLights.push([W * 0.24, H - 0.16, L * 0.24, spotIntensity]);
      pointLights.push([W * 0.24, H - 0.16, -L * 0.24, spotIntensity * 0.75]);
    }

    for (const item of (currentDesign.furniture || [])) {
      const cached = furnitureCache.get(item.id);
      if (!cached) continue;
      const rotRad = (item.rotationDeg || 0) * Math.PI / 180;
      const model = global.Mat4.multiply(
        global.Mat4.translate(item.xMeters, item.yMeters || 0, item.zMeters),
        global.Mat4.rotateY(rotRad)
      );
      const isSel = selectedTarget.type === 'FURNITURE' && selectedTarget.id === item.id;
      const primaryMat = resolveMaterialEntry(item.material);
      const itemCounterMat = item.secondaryMaterial ? resolveMaterialEntry(item.secondaryMaterial) : counterMat;

      if (currentDesign.realisticView) {
        if (item.kind === 'UPPER_CABINET') {
          const ledBoost = currentDesign.lightingPreset === 'INDOOR_SPOTLIGHTS' ? 2.4 : 1.55;
          pointLights.push([item.xMeters, Math.max(0.98, (item.yMeters || 1.48) - 0.14), item.zMeters + 0.16, ledBoost]);
        } else if (item.kind === 'FLOOR_LAMP' || item.kind === 'NIGHTSTAND') {
          const lBoost = currentDesign.lightingPreset === 'INDOOR_SPOTLIGHTS' ? 2.2 : 1.05;
          pointLights.push([item.xMeters, (item.yMeters || 0) + item.heightMeters + 0.1, item.zMeters, lBoost]);
        }
      }

      for (const part of cached.parts) {
        let matEntry = primaryMat;
        let isPartSel = isSel;
        if (part.role === 'countertop') {
          matEntry = itemCounterMat;
          if (selectedTarget.type === 'COUNTERTOPS') isPartSel = true;
        } else if (part.role === 'metal') {
          matEntry = metalMat;
        } else if (part.role === 'fabric_alt') {
          matEntry = fabricAltMat;
        } else if (part.role === 'dark_screen') {
          matEntry = darkScreenMat;
        } else if (part.role === 'lamp_glow') {
          matEntry = lampMat;
        } else if (part.role === 'wood_legs') {
          matEntry = woodLegsMat;
        }

        renderList.push({
          gpuMesh: part.gpuMesh,
          modelMatrix: model,
          matEntry,
          opacity: 1.0,
          emissive: part.emissive || 0.0,
          castShadow: true,
          selected: isPartSel
        });
      }
    }

    return { renderList, pointLights };
  }

  function renderOneFrame(timeSec) {
    if (!renderer || !currentDesign) return;
    resizeCanvas();

    if (animCam) {
      const t = Math.min(1, (performance.now() - animCam.start) / animCam.dur);
      const ease = 1 - Math.pow(1 - t, 3);
      cam.azimuth = animCam.fromAz + (animCam.toAz - animCam.fromAz) * ease;
      cam.elevation = animCam.fromEl + (animCam.toEl - animCam.fromEl) * ease;
      cam.distance = animCam.fromDist + (animCam.toDist - animCam.fromDist) * ease;
      cam.fov = animCam.fromFov + (animCam.toFov - animCam.fromFov) * ease;
      cam.target[0] = animCam.fromT[0] + (animCam.toT[0] - animCam.fromT[0]) * ease;
      cam.target[1] = animCam.fromT[1] + (animCam.toT[1] - animCam.fromT[1]) * ease;
      cam.target[2] = animCam.fromT[2] + (animCam.toT[2] - animCam.fromT[2]) * ease;
      if (t >= 1) animCam = null;
    }

    const eye = getCameraEye(cam);
    const aspect = canvas.width / Math.max(1, canvas.height);
    const proj = global.Mat4.perspective(cam.fov, aspect, 0.08, 60.0);
    const view = global.Mat4.lookAt(eye, cam.target, [0, 1, 0]);

    const frame = buildRenderFrameData(eye);
    renderer.render(frame.renderList, eye, proj, view, currentDesign, frame.pointLights, timeSec);
  }

  function loop(nowMs) {
    renderOneFrame(nowMs * 0.001);
    requestAnimationFrame(loop);
  }

  function raycastTap(clientX, clientY) {
    if (!currentDesign) return;
    const rect = canvas.getBoundingClientRect();
    const nx = ((clientX - rect.left) / rect.width) * 2 - 1;
    const ny = 1 - ((clientY - rect.top) / rect.height) * 2;

    const eye = getCameraEye(cam);
    const aspect = canvas.width / Math.max(1, canvas.height);
    const proj = global.Mat4.perspective(cam.fov, aspect, 0.08, 60.0);
    const view = global.Mat4.lookAt(eye, cam.target, [0, 1, 0]);
    const invVP = global.Mat4.invert(global.Mat4.multiply(proj, view));

    function unproject(x, y, z) {
      const v = [
        invVP[0]*x + invVP[4]*y + invVP[8]*z + invVP[12],
        invVP[1]*x + invVP[5]*y + invVP[9]*z + invVP[13],
        invVP[2]*x + invVP[6]*y + invVP[10]*z + invVP[14],
        invVP[3]*x + invVP[7]*y + invVP[11]*z + invVP[15]
      ];
      const w = v[3] || 1;
      return [v[0]/w, v[1]/w, v[2]/w];
    }

    const pNear = unproject(nx, ny, -1);
    const pFar = unproject(nx, ny, 1);
    let dx = pFar[0] - pNear[0], dy = pFar[1] - pNear[1], dz = pFar[2] - pNear[2];
    const len = Math.hypot(dx, dy, dz) || 1;
    dx /= len; dy /= len; dz /= len;

    let bestT = 1e9;
    let bestHit = null;

    for (const item of (currentDesign.furniture || [])) {
      const rot = (item.rotationDeg || 0) * Math.PI / 180;
      const cos = Math.cos(-rot), sin = Math.sin(-rot);
      const ox = eye[0] - item.xMeters;
      const oy = eye[1] - (item.yMeters || 0);
      const oz = eye[2] - item.zMeters;

      const ro = [ox * cos + oz * sin, oy, -ox * sin + oz * cos];
      const rd = [dx * cos + dz * sin, dy, -dx * sin + dz * cos];

      const minB = [-item.widthMeters * 0.5, 0, -item.depthMeters * 0.5];
      const maxB = [item.widthMeters * 0.5, Math.max(0.08, item.heightMeters), item.depthMeters * 0.5];

      let tMin = 0.05, tMax = 100.0;
      let hit = true;
      for (let a = 0; a < 3; a++) {
        if (Math.abs(rd[a]) < 1e-6) {
          if (ro[a] < minB[a] || ro[a] > maxB[a]) { hit = false; break; }
        } else {
          let t1 = (minB[a] - ro[a]) / rd[a];
          let t2 = (maxB[a] - ro[a]) / rd[a];
          if (t1 > t2) { const tmp = t1; t1 = t2; t2 = tmp; }
          tMin = Math.max(tMin, t1);
          tMax = Math.min(tMax, t2);
          if (tMin > tMax) { hit = false; break; }
        }
      }
      if (hit && tMin < bestT) {
        bestT = tMin;
        bestHit = { type: 'FURNITURE', id: item.id };
      }
    }

    const W = currentDesign.widthMeters;
    const L = currentDesign.lengthMeters;
    const H = currentDesign.wallHeightMeters;
    if (Math.abs(dy) > 1e-4) {
      const tFloor = (0 - eye[1]) / dy;
      if (tFloor > 0.1 && tFloor < bestT) {
        const hx = eye[0] + dx * tFloor;
        const hz = eye[2] + dz * tFloor;
        if (Math.abs(hx) <= W * 0.5 + 0.15 && Math.abs(hz) <= L * 0.5 + 0.15) {
          bestT = tFloor;
          bestHit = { type: 'FLOOR', id: '' };
        }
      }
    }

    const isTopView = cam.elevation > 1.25;
    const isInside = Math.abs(eye[0]) < W * 0.49 && Math.abs(eye[2]) < L * 0.49 && eye[1] < H - 0.04;
    const wallPlanes = [
      { axis: 2, val: -L * 0.5, normal: [0, 0, 1], spanAxis: 0, halfSpan: W * 0.5 },
      { axis: 2, val: L * 0.5, normal: [0, 0, -1], spanAxis: 0, halfSpan: W * 0.5 },
      { axis: 0, val: -W * 0.5, normal: [1, 0, 0], spanAxis: 2, halfSpan: L * 0.5 },
      { axis: 0, val: W * 0.5, normal: [-1, 0, 0], spanAxis: 2, halfSpan: L * 0.5 }
    ];
    for (const wp of wallPlanes) {
      const dot = (eye[0] - (wp.axis === 0 ? wp.val : 0)) * wp.normal[0] +
                  (eye[2] - (wp.axis === 2 ? wp.val : 0)) * wp.normal[2];
      if (!isTopView && !isInside && dot < -0.15) continue;
      const dAxis = wp.axis === 0 ? dx : dz;
      const eAxis = wp.axis === 0 ? eye[0] : eye[2];
      if (Math.abs(dAxis) > 1e-4) {
        const t = (wp.val - eAxis) / dAxis;
        if (t > 0.1 && t < bestT) {
          const hy = eye[1] + dy * t;
          const hSpan = wp.spanAxis === 0 ? (eye[0] + dx * t) : (eye[2] + dz * t);
          if (hy >= 0 && hy <= H && Math.abs(hSpan) <= wp.halfSpan) {
            bestT = t;
            bestHit = { type: 'WALLS', id: '' };
          }
        }
      }
    }

    if (bestHit && global.AndroidBridge && global.AndroidBridge.onElementSelected) {
      global.AndroidBridge.onElementSelected(bestHit.type, bestHit.id);
    }
  }

  const pointers = new Map();
  let tapStart = null;
  let prevPinchDist = null;
  let prevMid = null;

  canvas.addEventListener('pointerdown', (e) => {
    canvas.setPointerCapture(e.pointerId);
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    animCam = null;
    if (pointers.size === 1) {
      tapStart = { x: e.clientX, y: e.clientY, moved: false };
      prevPinchDist = null;
    } else if (pointers.size === 2) {
      tapStart = null;
      const pts = Array.from(pointers.values());
      prevPinchDist = Math.hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y);
      prevMid = { x: (pts[0].x + pts[1].x) * 0.5, y: (pts[0].y + pts[1].y) * 0.5 };
    }
  });

  canvas.addEventListener('pointermove', (e) => {
    const prev = pointers.get(e.pointerId);
    if (!prev) return;
    const dx = e.clientX - prev.x;
    const dy = e.clientY - prev.y;
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

    if (pointers.size === 1) {
      if (tapStart && Math.hypot(e.clientX - tapStart.x, e.clientY - tapStart.y) > 8) {
        tapStart.moved = true;
      }
      cam.azimuth -= dx * 0.0075;
      cam.elevation = Math.max(-0.22, Math.min(1.52, cam.elevation + dy * 0.007));
    } else if (pointers.size === 2) {
      const pts = Array.from(pointers.values());
      const dist = Math.hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y);
      const mid = { x: (pts[0].x + pts[1].x) * 0.5, y: (pts[0].y + pts[1].y) * 0.5 };
      if (prevPinchDist) {
        const scale = prevPinchDist / Math.max(10, dist);
        cam.distance = Math.max(0.6, Math.min(18.0, cam.distance * scale));
      }
      if (prevMid) {
        const mdx = (mid.x - prevMid.x) * 0.005 * Math.max(0.5, cam.distance * 0.25);
        const mdy = (mid.y - prevMid.y) * 0.005 * Math.max(0.5, cam.distance * 0.25);
        const cosA = Math.cos(cam.azimuth), sinA = Math.sin(cam.azimuth);
        cam.target[0] -= mdx * cosA + mdy * sinA;
        cam.target[2] -= -mdx * sinA + mdy * cosA;
      }
      prevPinchDist = dist;
      prevMid = mid;
    }
  });

  function onPointerUp(e) {
    if (pointers.size === 1 && tapStart && !tapStart.moved) {
      raycastTap(e.clientX, e.clientY);
    }
    pointers.delete(e.pointerId);
    if (pointers.size < 2) {
      prevPinchDist = null;
      prevMid = null;
    }
  }
  canvas.addEventListener('pointerup', onPointerUp);
  canvas.addEventListener('pointercancel', onPointerUp);

  canvas.addEventListener('wheel', (e) => {
    e.preventDefault();
    cam.distance = Math.max(0.6, Math.min(18.0, cam.distance * (1 + e.deltaY * 0.001)));
  }, { passive: false });

  let lastRoomSig = '';
  global.updateScene = function (jsonString) {
    try {
      const payload = JSON.parse(jsonString);
      currentDesign = payload.design;
      selectedTarget = payload.selectedTarget || { type: 'NONE', id: '' };

      const roomSig = JSON.stringify({
        w: currentDesign.widthMeters,
        l: currentDesign.lengthMeters,
        h: currentDesign.wallHeightMeters,
        o: currentDesign.openings
      });
      if (roomSig !== lastRoomSig) {
        lastRoomSig = roomSig;
        rebuildRoomMeshes();
      }
      syncFurnitureMeshes();
    } catch (err) {
      console.error('updateScene error', err);
    }
  };

  global.setCameraPreset = function (preset) {
    if (!currentDesign) return;
    const W = currentDesign.widthMeters;
    const L = currentDesign.lengthMeters;
    const H = currentDesign.wallHeightMeters;
    const maxDim = Math.max(W, L);
    let toAz = 0.72, toEl = 0.54, toDist = maxDim * 1.42, toT = [0, H * 0.35, 0];
    let toFov = 46 * Math.PI / 180;

    if (preset === 'TOP_FLOORPLAN') {
      toAz = 0.001;
      toEl = 1.54;
      toDist = maxDim * 1.55;
      toT = [0, 0.2, 0];
      toFov = 44 * Math.PI / 180;
    } else if (preset === 'CORNER_EYE_LEVEL') {
      // Enclosed interior corner eye-level perspective (shows all 4 walls + ceiling)
      const eyeX = W * 0.41;
      const eyeY = Math.min(1.48, H * 0.55);
      const eyeZ = L * 0.41;
      toT = [-W * 0.14, H * 0.38, -L * 0.14];
      const dx = eyeX - toT[0];
      const dy = eyeY - toT[1];
      const dz = eyeZ - toT[2];
      toDist = Math.hypot(dx, dy, dz);
      toAz = Math.atan2(dx, dz);
      toEl = Math.asin(dy / Math.max(0.1, toDist));
      toFov = 58 * Math.PI / 180; // Architectural interior wide lens
    }

    animCam = {
      start: performance.now(),
      dur: 520,
      fromAz: cam.azimuth, toAz,
      fromEl: cam.elevation, toEl,
      fromDist: cam.distance, toDist,
      fromFov: cam.fov, toFov,
      fromT: cam.target.slice(), toT
    };
  };

  global.captureSnapshot = function () {
    if (!renderer || !currentDesign) return;
    const prevW = canvas.width;
    const prevH = canvas.height;
    const aspect = prevW / Math.max(1, prevH);
    const snapW = 1920;
    const snapH = Math.max(720, Math.round(snapW / aspect));
    resizeCanvas(snapW, snapH);
    renderOneFrame(performance.now() * 0.001);
    const dataUrl = canvas.toDataURL('image/png');
    resizeCanvas(prevW, prevH);
    renderOneFrame(performance.now() * 0.001);

    if (global.AndroidBridge && global.AndroidBridge.onSnapshotCaptured) {
      global.AndroidBridge.onSnapshotCaptured(dataUrl);
    }
  };

  requestAnimationFrame(loop);
  if (global.AndroidBridge && global.AndroidBridge.onReady) {
    global.AndroidBridge.onReady();
  }
})(window);
