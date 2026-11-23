// Procedural Architectural Room & Rich Multi-Part Furniture Mesh Builder
// Supports customizable cabinet styles (Handleless, Shaker, Glass Display, Open Shelf) + Doors/Drawers/Handles toggles
(function (global) {
  function createMeshBuilder() {
    const positions = [];
    const normals = [];
    const uvs = [];
    const indices = [];

    function pushVertex(px, py, pz, nx, ny, nz, u, v) {
      positions.push(px, py, pz);
      normals.push(nx, ny, nz);
      uvs.push(u, v);
      return positions.length / 3 - 1;
    }

    function addQuad(p0, p1, p2, p3, nx, ny, nz, uScale, vScale) {
      const us = uScale || 1;
      const vs = vScale || 1;
      const i0 = pushVertex(p0[0], p0[1], p0[2], nx, ny, nz, 0, 0);
      const i1 = pushVertex(p1[0], p1[1], p1[2], nx, ny, nz, us, 0);
      const i2 = pushVertex(p2[0], p2[1], p2[2], nx, ny, nz, us, vs);
      const i3 = pushVertex(p3[0], p3[1], p3[2], nx, ny, nz, 0, vs);
      indices.push(i0, i1, i2, i0, i2, i3);
    }

    function addBox(cx, cy, cz, w, h, d, uvTileScale) {
      const hx = w * 0.5;
      const hy = h * 0.5;
      const hz = d * 0.5;
      const ts = uvTileScale || 0.7;
      addQuad([cx - hx, cy - hy, cz + hz], [cx + hx, cy - hy, cz + hz], [cx + hx, cy + hy, cz + hz], [cx - hx, cy + hy, cz + hz], 0, 0, 1, w * ts, h * ts);
      addQuad([cx + hx, cy - hy, cz - hz], [cx - hx, cy - hy, cz - hz], [cx - hx, cy + hy, cz - hz], [cx + hx, cy + hy, cz - hz], 0, 0, -1, w * ts, h * ts);
      addQuad([cx + hx, cy - hy, cz + hz], [cx + hx, cy - hy, cz - hz], [cx + hx, cy + hy, cz - hz], [cx + hx, cy + hy, cz + hz], 1, 0, 0, d * ts, h * ts);
      addQuad([cx - hx, cy - hy, cz - hz], [cx - hx, cy - hy, cz + hz], [cx - hx, cy + hy, cz + hz], [cx - hx, cy + hy, cz - hz], -1, 0, 0, d * ts, h * ts);
      addQuad([cx - hx, cy + hy, cz + hz], [cx + hx, cy + hy, cz + hz], [cx + hx, cy + hy, cz - hz], [cx - hx, cy + hy, cz - hz], 0, 1, 0, w * ts, d * ts);
      addQuad([cx - hx, cy - hy, cz - hz], [cx + hx, cy - hy, cz - hz], [cx + hx, cy - hy, cz + hz], [cx - hx, cy - hy, cz + hz], 0, -1, 0, w * ts, d * ts);
    }

    function addCylinder(cx, cy, cz, radius, height, segments) {
      const segs = segments || 12;
      const hy = height * 0.5;
      const topCenter = pushVertex(cx, cy + hy, cz, 0, 1, 0, 0.5, 0.5);
      const botCenter = pushVertex(cx, cy - hy, cz, 0, -1, 0, 0.5, 0.5);
      const topRing = [];
      const botRing = [];
      for (let i = 0; i <= segs; i++) {
        const a = (i / segs) * Math.PI * 2;
        const cos = Math.cos(a);
        const sin = Math.sin(a);
        const px = cx + cos * radius;
        const pz = cz + sin * radius;
        topRing.push(pushVertex(px, cy + hy, pz, 0, 1, 0, 0.5 + cos * 0.5, 0.5 + sin * 0.5));
        botRing.push(pushVertex(px, cy - hy, pz, 0, -1, 0, 0.5 + cos * 0.5, 0.5 + sin * 0.5));
      }
      for (let i = 0; i < segs; i++) {
        indices.push(topCenter, topRing[i + 1], topRing[i]);
        indices.push(botCenter, botRing[i], botRing[i + 1]);
      }
      for (let i = 0; i < segs; i++) {
        const a0 = (i / segs) * Math.PI * 2;
        const a1 = ((i + 1) / segs) * Math.PI * 2;
        const nx0 = Math.cos(a0), nz0 = Math.sin(a0);
        const nx1 = Math.cos(a1), nz1 = Math.sin(a1);
        const p0 = pushVertex(cx + nx0 * radius, cy - hy, cz + nz0 * radius, nx0, 0, nz0, i / segs, 0);
        const p1 = pushVertex(cx + nx1 * radius, cy - hy, cz + nz1 * radius, nx1, 0, nz1, (i + 1) / segs, 0);
        const p2 = pushVertex(cx + nx1 * radius, cy + hy, cz + nz1 * radius, nx1, 0, nz1, (i + 1) / segs, 1);
        const p3 = pushVertex(cx + nx0 * radius, cy + hy, cz + nz0 * radius, nx0, 0, nz0, i / segs, 1);
        indices.push(p0, p1, p2, p0, p2, p3);
      }
    }

    return {
      addBox,
      addCylinder,
      build: function () {
        return {
          positions: new Float32Array(positions),
          normals: new Float32Array(normals),
          uvs: new Float32Array(uvs),
          indices: new Uint16Array(indices)
        };
      }
    };
  }

  function buildWallWithOpenings(wallSpan, wallHeight, wallThickness, openings) {
    const b = createMeshBuilder();
    const sorted = (openings || []).slice().sort((a, c) => a.offsetMeters - c.offsetMeters);
    const halfSpan = wallSpan * 0.5;
    let cursor = -halfSpan;

    for (const op of sorted) {
      const opW = Math.min(op.widthMeters, wallSpan - 0.3);
      const opLeft = Math.max(-halfSpan + 0.1, Math.min(halfSpan - 0.1 - opW, op.offsetMeters - opW * 0.5));
      const opRight = opLeft + opW;
      const sill = op.type === 'DOOR' ? 0 : Math.max(0.15, Math.min(wallHeight - 0.5, op.sillHeightMeters || 0.9));
      const opTop = Math.min(wallHeight - 0.12, sill + (op.heightMeters || (op.type === 'DOOR' ? 2.1 : 1.2)));

      if (opLeft > cursor + 0.005) {
        const segW = opLeft - cursor;
        b.addBox(cursor + segW * 0.5, wallHeight * 0.5, 0, segW, wallHeight, wallThickness, 0.8);
      }
      if (sill > 0.01) {
        b.addBox((opLeft + opRight) * 0.5, sill * 0.5, 0, opW, sill, wallThickness, 0.8);
      }
      if (wallHeight - opTop > 0.01) {
        const lintelH = wallHeight - opTop;
        b.addBox((opLeft + opRight) * 0.5, opTop + lintelH * 0.5, 0, opW, lintelH, wallThickness, 0.8);
      }
      cursor = Math.max(cursor, opRight);
    }

    if (halfSpan - cursor > 0.005) {
      const segW = halfSpan - cursor;
      b.addBox(cursor + segW * 0.5, wallHeight * 0.5, 0, segW, wallHeight, wallThickness, 0.8);
    }

    return b.build();
  }

  // Builds a true hollow open-shelving compartment (back, sides, top, bottom, inner shelves & dividers)
  function addOpenCarcass(builder, backB, cx, baseY, cz, w, h, d, shelvesCount, colsCount, openFaceAxis, openFaceSign) {
    const t = 0.024;
    const innerH = Math.max(0.05, h - t * 2);
    const innerW = Math.max(0.05, w - t * 2);
    const innerD = Math.max(0.05, d - t);

    // Bottom & Top boards
    builder.addBox(cx, baseY + t * 0.5, cz, w, t, d, 0.8);
    builder.addBox(cx, baseY + h - t * 0.5, cz, w, t, d, 0.8);

    if (openFaceAxis === 'X') {
      // Open facing +/-X
      builder.addBox(cx - openFaceSign * (w * 0.5 - t * 0.5), baseY + h * 0.5, cz, t, innerH, d, 0.8);
      builder.addBox(cx, baseY + h * 0.5, cz - d * 0.5 + t * 0.5, w, innerH, t, 0.8);
      builder.addBox(cx, baseY + h * 0.5, cz + d * 0.5 - t * 0.5, w, innerH, t, 0.8);
      for (let s = 1; s < shelvesCount; s++) {
        const sy = baseY + (s / shelvesCount) * h;
        builder.addBox(cx + openFaceSign * t * 0.3, sy, cz, innerW, t * 0.85, innerD, 0.8);
      }
      for (let c = 1; c < colsCount; c++) {
        const sz = cz - d * 0.5 + (c / colsCount) * d;
        builder.addBox(cx + openFaceSign * t * 0.3, baseY + h * 0.5, sz, innerW, innerH, t * 0.85, 0.8);
      }
    } else {
      // Open facing +/-Z
      const backTarget = backB || builder;
      backTarget.addBox(cx, baseY + h * 0.5, cz - openFaceSign * (d * 0.5 - t * 0.5), w, innerH, t, 0.8);
      builder.addBox(cx - w * 0.5 + t * 0.5, baseY + h * 0.5, cz, t, innerH, d, 0.8);
      builder.addBox(cx + w * 0.5 - t * 0.5, baseY + h * 0.5, cz, t, innerH, d, 0.8);
      for (let s = 1; s < shelvesCount; s++) {
        const sy = baseY + (s / shelvesCount) * h;
        builder.addBox(cx, sy, cz + openFaceSign * t * 0.3, innerW, t * 0.85, innerD, 0.8);
      }
      for (let c = 1; c < colsCount; c++) {
        const sx = cx - w * 0.5 + (c / colsCount) * w;
        builder.addBox(sx, baseY + h * 0.5, cz + openFaceSign * t * 0.3, t * 0.85, innerH, innerD, 0.8);
      }
    }
  }

  // Adds a styled front door (Handleless slab, Shaker raised frame, Glass display + Aluminum Profile / Bar handle)
  function addStyledDoor(bodyB, darkGlassB, profileB, cx, cy, cz, spanW, spanH, style, showHandles, handleSideSign, faceAxis, faceSign, handleType) {
    const fw = Math.max(0.04, Math.min(0.06, spanW * 0.14));
    const fh = Math.max(0.04, Math.min(0.06, spanH * 0.12));
    const hMode = handleType || (showHandles ? 'SLIM_BAR' : 'GOLA_CHANNEL');
    const drawLipProfile = hMode === 'PROFILE_EDGE';
    const drawBarHandle = showHandles || hMode === 'SLIM_BAR';

    if (faceAxis === 'X') {
      if (style === 'GLASS_DISPLAY') {
        bodyB.addBox(cx, cy + spanH * 0.5 - fh * 0.5, cz, 0.022, fh, spanW, 0.85);
        bodyB.addBox(cx, cy - spanH * 0.5 + fh * 0.5, cz, 0.022, fh, spanW, 0.85);
        bodyB.addBox(cx, cy, cz - spanW * 0.5 + fw * 0.5, 0.022, spanH - fh * 2, fw, 0.85);
        bodyB.addBox(cx, cy, cz + spanW * 0.5 - fw * 0.5, 0.022, spanH - fh * 2, fw, 0.85);
        darkGlassB.addBox(cx - faceSign * 0.002, cy, cz, 0.014, spanH - fh * 2, spanW - fw * 2, 1.0);
      } else if (style === 'SHAKER_FRAME') {
        bodyB.addBox(cx, cy, cz, 0.016, spanH, spanW, 0.85);
        bodyB.addBox(cx + faceSign * 0.008, cy + spanH * 0.5 - fh * 0.5, cz, 0.014, fh, spanW, 0.85);
        bodyB.addBox(cx + faceSign * 0.008, cy - spanH * 0.5 + fh * 0.5, cz, 0.014, fh, spanW, 0.85);
        bodyB.addBox(cx + faceSign * 0.008, cy, cz - spanW * 0.5 + fw * 0.5, 0.014, spanH - fh * 2, fw, 0.85);
        bodyB.addBox(cx + faceSign * 0.008, cy, cz + spanW * 0.5 - fw * 0.5, 0.014, spanH - fh * 2, fw, 0.85);
      } else {
        bodyB.addBox(cx, cy, cz, 0.02, spanH, spanW, 0.85);
      }
      if (drawLipProfile && hMode !== 'PUSH_OPEN') {
        profileB.addBox(cx + faceSign * 0.014, cy + spanH * 0.5 - 0.01, cz, 0.022, 0.02, spanW, 1.0);
      } else if (drawBarHandle && hMode !== 'PUSH_OPEN') {
        const hz = cz + handleSideSign * (spanW * 0.36);
        const hLen = Math.min(0.24, spanH * 0.32);
        profileB.addBox(cx + faceSign * 0.022, cy + spanH * 0.14, hz, 0.024, hLen, 0.014, 1.0);
      }
      return;
    }

    // Facing Z
    if (style === 'GLASS_DISPLAY') {
      bodyB.addBox(cx, cy + spanH * 0.5 - fh * 0.5, cz, spanW, fh, 0.022, 0.85);
      bodyB.addBox(cx, cy - spanH * 0.5 + fh * 0.5, cz, spanW, fh, 0.022, 0.85);
      bodyB.addBox(cx - spanW * 0.5 + fw * 0.5, cy, cz, fw, spanH - fh * 2, 0.022, 0.85);
      bodyB.addBox(cx + spanW * 0.5 - fw * 0.5, cy, cz, fw, spanH - fh * 2, 0.022, 0.85);
      darkGlassB.addBox(cx, cy, cz - faceSign * 0.002, spanW - fw * 2, spanH - fh * 2, 0.014, 1.0);
    } else if (style === 'SHAKER_FRAME') {
      bodyB.addBox(cx, cy, cz, spanW, spanH, 0.016, 0.85);
      bodyB.addBox(cx, cy + spanH * 0.5 - fh * 0.5, cz + faceSign * 0.008, spanW, fh, 0.014, 0.85);
      bodyB.addBox(cx, cy - spanH * 0.5 + fh * 0.5, cz + faceSign * 0.008, spanW, fh, 0.014, 0.85);
      bodyB.addBox(cx - spanW * 0.5 + fw * 0.5, cy, cz + faceSign * 0.008, fw, spanH - fh * 2, 0.014, 0.85);
      bodyB.addBox(cx + spanW * 0.5 - fw * 0.5, cy, cz + faceSign * 0.008, fw, spanH - fh * 2, 0.014, 0.85);
    } else {
      bodyB.addBox(cx, cy, cz, spanW, spanH, 0.02, 0.85);
    }
    if (drawLipProfile && hMode !== 'PUSH_OPEN') {
      profileB.addBox(cx, cy + spanH * 0.5 - 0.01, cz + faceSign * 0.014, spanW, 0.02, 0.022, 1.0);
    } else if (drawBarHandle && hMode !== 'PUSH_OPEN') {
      const hx = cx + handleSideSign * (spanW * 0.36);
      const hLen = Math.min(0.24, spanH * 0.32);
      profileB.addBox(hx, cy + spanH * 0.14, cz + faceSign * 0.022, 0.014, hLen, 0.024, 1.0);
    }
  }

  function addStyledDrawerStack(bodyB, darkTrimB, profileB, cx, baseY, cz, dw, totalH, rows, style, showHandles, handleType) {
    const drH = totalH / rows;
    const hMode = handleType || (showHandles ? 'SLIM_BAR' : 'GOLA_CHANNEL');
    const drawLipProfile = hMode === 'PROFILE_EDGE';
    const drawBarHandle = showHandles || hMode === 'SLIM_BAR';
    for (let r = 0; r < rows; r++) {
      const ry = baseY + drH * (r + 0.5);
      const hPanel = drH - 0.016;
      if (style === 'SHAKER_FRAME') {
        const fw = Math.min(0.05, dw * 0.14);
        const fh = Math.min(0.038, hPanel * 0.24);
        bodyB.addBox(cx, ry, cz, dw, hPanel, 0.016, 0.85);
        bodyB.addBox(cx, ry + hPanel * 0.5 - fh * 0.5, cz + 0.007, dw, fh, 0.012, 0.85);
        bodyB.addBox(cx, ry - hPanel * 0.5 + fh * 0.5, cz + 0.007, dw, fh, 0.012, 0.85);
        bodyB.addBox(cx - dw * 0.5 + fw * 0.5, ry, cz + 0.007, fw, hPanel - fh * 2, 0.012, 0.85);
        bodyB.addBox(cx + dw * 0.5 - fw * 0.5, ry, cz + 0.007, fw, hPanel - fh * 2, 0.012, 0.85);
      } else {
        bodyB.addBox(cx, ry, cz, dw, hPanel, 0.02, 0.85);
      }
      if (r > 0) {
        darkTrimB.addBox(cx, baseY + drH * r, cz + 0.004, dw, 0.012, 0.016, 1.0);
      }
      if (drawLipProfile && hMode !== 'PUSH_OPEN') {
        profileB.addBox(cx, ry + hPanel * 0.5 - 0.009, cz + 0.014, dw, 0.018, 0.022, 1.0);
      } else if (drawBarHandle && hMode !== 'PUSH_OPEN') {
        profileB.addBox(cx, ry, cz + 0.024, Math.min(0.14, dw * 0.42), 0.014, 0.02, 1.0);
      }
    }
  }

  function addKitchenSinkAndFaucet(sinkB, faucetB, cx, h, cz, specs) {
    const sw = Math.min(1.0, (specs && specs.sinkWidthM) ? specs.sinkWidthM : 0.58);
    const isDouble = !!(specs && specs.sinkDoubleBowl);
    sinkB.addBox(cx, h + 0.004, cz, sw, 0.012, 0.43, 1.0);
    if (isDouble) {
      sinkB.addBox(cx, h + 0.012, cz, 0.028, 0.01, 0.38, 1.0);
    }
    const isSpring = !!(specs && specs.faucetIsSpringChef);
    if (isSpring) {
      faucetB.addCylinder(cx, h + 0.16, cz - 0.18, 0.015, 0.32, 10);
      faucetB.addBox(cx, h + 0.32, cz - 0.10, 0.022, 0.022, 0.18, 1.0);
      faucetB.addCylinder(cx, h + 0.25, cz - 0.02, 0.018, 0.12, 10);
    } else {
      faucetB.addCylinder(cx, h + 0.13, cz - 0.18, 0.014, 0.26, 10);
      faucetB.addBox(cx, h + 0.26, cz - 0.10, 0.024, 0.024, 0.16, 1.0);
    }
  }

  function addBuiltInGasHob(darkB, metalB, cx, h, cz) {
    darkB.addBox(cx, h + 0.005, cz, 0.72, 0.012, 0.50, 1.0);
    [[-0.20, -0.11], [0.20, -0.11], [0.0, 0.0], [-0.20, 0.12], [0.20, 0.12]].forEach(([bx, bz]) => {
      metalB.addCylinder(cx + bx, h + 0.015, cz + bz, 0.065, 0.012, 12);
    });
  }

  function addChair(builder, cx, cz, rotSign) {
    const seatH = 0.45;
    builder.addBox(cx, seatH, cz, 0.42, 0.04, 0.42, 0.9);
    builder.addBox(cx - 0.17, seatH * 0.5, cz - 0.17, 0.035, seatH, 0.035, 1.0);
    builder.addBox(cx + 0.17, seatH * 0.5, cz - 0.17, 0.035, seatH, 0.035, 1.0);
    builder.addBox(cx - 0.17, seatH * 0.5, cz + 0.17, 0.035, seatH, 0.035, 1.0);
    builder.addBox(cx + 0.17, seatH * 0.5, cz + 0.17, 0.035, seatH, 0.035, 1.0);
    builder.addBox(cx, seatH + 0.21, cz + rotSign * 0.19, 0.42, 0.40, 0.035, 0.9);
  }

  function addStool(metalB, seatB, cx, cz, h) {
    const seatH = h - 0.24;
    metalB.addCylinder(cx - 0.13, seatH * 0.5, cz - 0.13, 0.015, seatH, 8);
    metalB.addCylinder(cx + 0.13, seatH * 0.5, cz - 0.13, 0.015, seatH, 8);
    metalB.addCylinder(cx - 0.13, seatH * 0.5, cz + 0.13, 0.015, seatH, 8);
    metalB.addCylinder(cx + 0.13, seatH * 0.5, cz + 0.13, 0.015, seatH, 8);
    metalB.addBox(cx, seatH * 0.35, cz, 0.28, 0.016, 0.28, 1.0);
    seatB.addCylinder(cx, seatH + 0.025, cz, 0.18, 0.05, 14);
  }

  function buildFurnitureParts(item, kitchenSpecs) {
    const specs = kitchenSpecs || {};
    const handleType = specs.handleType || 'PROFILE_EDGE';
    const counterThickness = Math.max(0.02, Math.min(0.055, (specs.countertopThicknessCm || 3) / 100.0));
    const hasHob = Array.isArray(specs.organizers) && specs.organizers.includes('BUILT_IN_HOB_HOOD');
    const useAlumPlinth = specs.aluminumPlinth !== false;
    const useUnderLed = specs.underCabinetLed !== false;

    const w = Math.max(0.25, item.widthMeters);
    const d = Math.max(0.25, item.depthMeters);
    const h = Math.max(0.02, item.heightMeters);
    const style = item.style || 'HANDLELESS_GOLA';
    const isOpenShelf = style === 'OPEN_SHELVING';
    const showDoors = !isOpenShelf && (item.showDoors !== false);
    const showDrawers = !isOpenShelf && (item.showDrawers !== false);
    const showHandles = item.showHandles === true;

    const parts = [];
    function pushPart(builder, role, emissive) {
      const built = builder.build();
      if (built.indices.length === 0) return;
      parts.push({
        mesh: built,
        role: role || 'primary',
        emissive: emissive || 0.0
      });
    }

    switch (item.kind) {
      case 'STRAIGHT_COUNTER': {
        const kickH = 0.09;
        const topH = counterThickness;
        const golaH = (style === 'HANDLELESS_GOLA' && handleType === 'GOLA_CHANNEL' && (showDoors || showDrawers)) ? 0.026 : 0.0;
        const bodyH = Math.max(0.3, h - kickH - topH);
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();
        const plinthB = createMeshBuilder();
        const profileB = createMeshBuilder();
        const sinkB = createMeshBuilder();
        const faucetB = createMeshBuilder();
        const metalB = createMeshBuilder();

        plinthB.addBox(0, kickH * 0.5, -0.03, w - 0.04, kickH, d - 0.08, 0.9);
        if (golaH > 0) {
          profileB.addBox(0, h - topH - golaH * 0.5, 0, w, golaH, d - 0.01, 0.9);
        }

        const bays = Math.max(2, Math.round(w / 0.50));
        const dw = (w - 0.02) / bays;
        const frontH = bodyH - golaH;

        if (!showDoors && !showDrawers) {
          addOpenCarcass(bodyB, darkTrimB, 0, kickH, -0.01, w, frontH, d - 0.02, 2, bays, 'Z', 1);
        } else if (!showDoors && showDrawers) {
          const centerBay = Math.floor(bays / 2);
          for (let i = 0; i < bays; i++) {
            const dx = -w * 0.5 + 0.01 + dw * (i + 0.5);
            if (i === centerBay) {
              bodyB.addBox(dx, kickH + frontH * 0.5, -0.01, dw, frontH, d - 0.02, 0.8);
              addStyledDrawerStack(bodyB, darkTrimB, profileB, dx, kickH + 0.01, d * 0.5 - 0.004, dw - 0.012, frontH - 0.02, 3, style, showHandles, handleType);
            } else {
              addOpenCarcass(bodyB, darkTrimB, dx, kickH, -0.01, dw, frontH, d - 0.02, 2, 1, 'Z', 1);
            }
          }
        } else {
          bodyB.addBox(0, kickH + frontH * 0.5, -0.01, w, frontH, d - 0.02, 0.8);
          for (let i = 0; i < bays; i++) {
            const dx = -w * 0.5 + 0.01 + dw * (i + 0.5);
            if (showDrawers && i === Math.floor(bays / 2) && bays >= 3) {
              addStyledDrawerStack(bodyB, darkTrimB, profileB, dx, kickH + 0.01, d * 0.5 - 0.004, dw - 0.012, frontH - 0.02, 3, style, showHandles, handleType);
            } else {
              addStyledDoor(bodyB, darkTrimB, profileB, dx, kickH + frontH * 0.5, d * 0.5 - 0.004, dw - 0.012, frontH - 0.02, style, showHandles, i % 2 === 0 ? 1 : -1, 'Z', 1, handleType);
            }
          }
        }

        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, 0.008, w + 0.02, topH, d + 0.02, 0.6);

        if (w >= 0.78) {
          const sinkX = w >= 2.0 ? -w * 0.28 : 0;
          addKitchenSinkAndFaucet(sinkB, faucetB, sinkX, h, -0.02, specs);
        }
        if (hasHob && w >= 1.8) {
          addBuiltInGasHob(darkTrimB, metalB, w * 0.24, h, -0.01);
        }

        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');
        pushPart(plinthB, useAlumPlinth ? 'aluminum_plinth' : 'dark_screen');
        pushPart(profileB, 'aluminum_profile');
        pushPart(topB, 'countertop');
        pushPart(sinkB, 'sink_basin');
        pushPart(faucetB, 'faucet_metal');
        pushPart(metalB, 'metal');
        break;
      }

      case 'L_COUNTER': {
        const kickH = 0.09;
        const topH = counterThickness;
        const golaH = (style === 'HANDLELESS_GOLA' && handleType === 'GOLA_CHANNEL' && (showDoors || showDrawers)) ? 0.026 : 0.0;
        const bodyH = Math.max(0.3, h - kickH - topH);
        const frontH = bodyH - golaH;
        const legDepth = Math.min(0.64, Math.min(w, d) * 0.42);
        const sideLen = Math.max(0.4, d - legDepth);

        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();
        const plinthB = createMeshBuilder();
        const profileB = createMeshBuilder();
        const sinkB = createMeshBuilder();
        const faucetB = createMeshBuilder();
        const metalB = createMeshBuilder();

        plinthB.addBox(0, kickH * 0.5, -d * 0.5 + legDepth * 0.5 - 0.02, w - 0.04, kickH, legDepth - 0.08, 0.8);
        plinthB.addBox(-w * 0.5 + legDepth * 0.5 - 0.02, kickH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.08, kickH, sideLen - 0.02, 0.8);
        if (golaH > 0) {
          profileB.addBox(0, h - topH - golaH * 0.5, -d * 0.5 + legDepth * 0.5, w, golaH, legDepth - 0.01, 0.8);
          profileB.addBox(-w * 0.5 + legDepth * 0.5, h - topH - golaH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.01, golaH, sideLen, 0.8);
        }

        const numFront = Math.max(4, Math.round((w - legDepth) / 0.48));
        const dw = (w - legDepth - 0.02) / numFront;
        const numSide = Math.max(2, Math.round(sideLen / 0.52));
        const sw = (sideLen - 0.02) / numSide;

        if (!showDoors && !showDrawers) {
          bodyB.addBox(-w * 0.5 + legDepth * 0.5, kickH + frontH * 0.5, -d * 0.5 + legDepth * 0.5, legDepth - 0.02, frontH, legDepth - 0.02, 0.8);
          addOpenCarcass(bodyB, darkTrimB, legDepth * 0.5, kickH, -d * 0.5 + legDepth * 0.5, w - legDepth, frontH, legDepth - 0.02, 2, numFront, 'Z', 1);
          addOpenCarcass(bodyB, darkTrimB, -w * 0.5 + legDepth * 0.5, kickH, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.02, frontH, sideLen, 2, numSide, 'X', 1);
        } else if (!showDoors && showDrawers) {
          bodyB.addBox(-w * 0.5 + legDepth * 0.5, kickH + frontH * 0.5, -d * 0.5 + legDepth * 0.5, legDepth - 0.02, frontH, legDepth - 0.02, 0.8);
          addOpenCarcass(bodyB, darkTrimB, -w * 0.5 + legDepth * 0.5, kickH, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.02, frontH, sideLen, 2, numSide, 'X', 1);
          const centerIdx = Math.floor(numFront / 2);
          for (let i = 0; i < numFront; i++) {
            const dx = -w * 0.5 + legDepth + 0.01 + dw * (i + 0.5);
            if (i === centerIdx) {
              bodyB.addBox(dx, kickH + frontH * 0.5, -d * 0.5 + legDepth * 0.5, dw, frontH, legDepth - 0.02, 0.8);
              addStyledDrawerStack(bodyB, darkTrimB, profileB, dx, kickH + 0.01, -d * 0.5 + legDepth - 0.002, dw - 0.012, frontH - 0.02, 3, style, showHandles, handleType);
            } else {
              addOpenCarcass(bodyB, darkTrimB, dx, kickH, -d * 0.5 + legDepth * 0.5, dw, frontH, legDepth - 0.02, 2, 1, 'Z', 1);
            }
          }
        } else {
          bodyB.addBox(0, kickH + frontH * 0.5, -d * 0.5 + legDepth * 0.5, w, frontH, legDepth - 0.02, 0.8);
          bodyB.addBox(-w * 0.5 + legDepth * 0.5, kickH + frontH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.02, frontH, sideLen, 0.8);
          for (let i = 0; i < numFront; i++) {
            const dx = -w * 0.5 + legDepth + 0.01 + dw * (i + 0.5);
            if (showDrawers && i === Math.floor(numFront / 2)) {
              addStyledDrawerStack(bodyB, darkTrimB, profileB, dx, kickH + 0.01, -d * 0.5 + legDepth - 0.002, dw - 0.012, frontH - 0.02, 3, style, showHandles, handleType);
            } else {
              addStyledDoor(bodyB, darkTrimB, profileB, dx, kickH + frontH * 0.5, -d * 0.5 + legDepth - 0.002, dw - 0.012, frontH - 0.02, style, showHandles, i % 2 === 0 ? 1 : -1, 'Z', 1, handleType);
            }
          }
          for (let i = 0; i < numSide; i++) {
            const dz = -d * 0.5 + legDepth + 0.01 + sw * (i + 0.5);
            addStyledDoor(bodyB, darkTrimB, profileB, -w * 0.5 + legDepth - 0.002, kickH + frontH * 0.5, dz, sw - 0.012, frontH - 0.02, style, showHandles, i % 2 === 0 ? 1 : -1, 'X', 1, handleType);
          }
        }

        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, -d * 0.5 + legDepth * 0.5, w + 0.02, topH, legDepth + 0.02, 0.6);
        topB.addBox(-w * 0.5 + legDepth * 0.5, h - topH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth + 0.02, topH, sideLen + 0.01, 0.6);

        addKitchenSinkAndFaucet(sinkB, faucetB, w * 0.24, h, -d * 0.5 + legDepth * 0.48, specs);
        if (hasHob) {
          addBuiltInGasHob(darkTrimB, metalB, -w * 0.14, h, -d * 0.5 + legDepth * 0.5);
        }

        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');
        pushPart(plinthB, useAlumPlinth ? 'aluminum_plinth' : 'dark_screen');
        pushPart(profileB, 'aluminum_profile');
        pushPart(topB, 'countertop');
        pushPart(sinkB, 'sink_basin');
        pushPart(faucetB, 'faucet_metal');
        pushPart(metalB, 'metal');
        break;
      }

      case 'KITCHEN_ISLAND': {
        const kickH = 0.09;
        const topH = counterThickness + 0.005;
        const golaH = (style === 'HANDLELESS_GOLA' && handleType === 'GOLA_CHANNEL' && showDoors) ? 0.024 : 0.0;
        const bodyH = Math.max(0.3, h - kickH - topH);
        const frontH = bodyH - golaH;
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();
        const plinthB = createMeshBuilder();
        const profileB = createMeshBuilder();
        const metalB = createMeshBuilder();

        plinthB.addBox(0, kickH * 0.5, -0.06, w - 0.06, kickH, d - 0.22, 0.8);
        if (golaH > 0) {
          profileB.addBox(0, h - topH - golaH * 0.5, -0.05, w - 0.02, golaH, d - 0.16, 0.8);
        }

        const doors = Math.max(2, Math.round(w / 0.55));
        const dw = (w - 0.06) / doors;

        if (!showDoors && !showDrawers) {
          addOpenCarcass(bodyB, darkTrimB, 0, kickH, -0.05, w - 0.02, frontH, d - 0.16, 2, doors, 'Z', -1);
        } else {
          bodyB.addBox(0, kickH + frontH * 0.5, -0.05, w - 0.02, frontH, d - 0.16, 0.8);
          for (let i = 0; i < doors; i++) {
            const dx = -w * 0.5 + 0.03 + dw * (i + 0.5);
            if (showDrawers && !showDoors) {
              addStyledDrawerStack(bodyB, darkTrimB, profileB, dx, kickH + 0.01, -d * 0.5 + 0.02, dw - 0.015, frontH - 0.02, 2, style, showHandles, handleType);
            } else {
              addStyledDoor(bodyB, darkTrimB, profileB, dx, kickH + frontH * 0.5, -d * 0.5 + 0.02, dw - 0.015, frontH - 0.03, style, showHandles, i % 2 === 0 ? 1 : -1, 'Z', -1, handleType);
            }
          }
        }
        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');
        pushPart(plinthB, useAlumPlinth ? 'aluminum_plinth' : 'dark_screen');
        pushPart(profileB, 'aluminum_profile');

        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, 0.02, w + 0.06, topH, d + 0.04, 0.6);
        pushPart(topB, 'countertop');

        const stoolSeats = createMeshBuilder();
        if (w >= 1.3) {
          addStool(metalB, stoolSeats, -w * 0.24, d * 0.42, h);
          addStool(metalB, stoolSeats, w * 0.24, d * 0.42, h);
        }
        stoolSeats.addCylinder(0.18, h + 0.045, 0.0, 0.14, 0.08, 16);
        pushPart(metalB, 'metal');
        pushPart(stoolSeats, 'fabric_alt');
        break;
      }

      case 'UPPER_CABINET': {
        const bodyB = createMeshBuilder();
        const darkGlassB = createMeshBuilder();
        const profileB = createMeshBuilder();
        const shelfW = (style === 'GLASS_DISPLAY' && w >= 1.4 && showDoors) ? 0.26 : 0.0;
        const mainW = w - shelfW;
        const mainCenterX = -shelfW * 0.5;
        const doors = Math.max(2, Math.round(mainW / 0.45));
        const dw = (mainW - 0.02) / doors;

        if (!showDoors) {
          addOpenCarcass(bodyB, null, 0, 0, 0, w, h, d - 0.02, h >= 0.7 ? 3 : 2, Math.max(2, Math.round(w / 0.55)), 'Z', 1);
        } else {
          bodyB.addBox(mainCenterX, h * 0.5, 0, mainW, h, d - 0.02, 0.85);
          const hasGlassStrip = style === 'GLASS_DISPLAY' && h >= 0.72;
          const glassH = hasGlassStrip ? h * 0.28 : 0.0;
          const upperDoorH = h - glassH - 0.02;

          for (let i = 0; i < doors; i++) {
            const dx = mainCenterX - mainW * 0.5 + 0.01 + dw * (i + 0.5);
            const doorStyle = (style === 'SHAKER_FRAME') ? 'SHAKER_FRAME' : (style === 'GLASS_DISPLAY' && !hasGlassStrip) ? 'GLASS_DISPLAY' : 'HANDLELESS_GOLA';
            addStyledDoor(bodyB, darkGlassB, profileB, dx, glassH + upperDoorH * 0.5 + 0.01, d * 0.5, dw - 0.01, upperDoorH - 0.01, doorStyle, showHandles, i % 2 === 0 ? 1 : -1, 'Z', 1, handleType);
            if (hasGlassStrip) {
              darkGlassB.addBox(dx, glassH * 0.5 + 0.005, d * 0.5, dw - 0.01, glassH - 0.01, 0.022, 1.0);
            }
          }
          if (shelfW > 0.1) {
            const sx = w * 0.5 - shelfW * 0.5;
            darkGlassB.addBox(sx, h * 0.5, -d * 0.42, shelfW, h, 0.02, 1.0);
            bodyB.addBox(sx + shelfW * 0.5 - 0.012, h * 0.5, 0, 0.024, h, d, 0.85);
            for (let r = 0; r <= 3; r++) {
              bodyB.addBox(sx, (r / 3) * (h - 0.024) + 0.012, 0, shelfW, 0.022, d, 0.85);
            }
          }
        }
        pushPart(bodyB, 'primary');
        pushPart(darkGlassB, 'dark_screen');
        pushPart(profileB, 'aluminum_profile');

        if (useUnderLed) {
          const ledB = createMeshBuilder();
          ledB.addBox(mainCenterX, 0.004, -d * 0.25, mainW - 0.08, 0.01, 0.028, 1.0);
          pushPart(ledB, 'lamp_glow', 0.98);
        }
        break;
      }

      case 'TALL_PANTRY': {
        const bodyB = createMeshBuilder();
        const darkGlassB = createMeshBuilder();
        const metalB = createMeshBuilder();

        if (!showDoors && !showDrawers) {
          addOpenCarcass(bodyB, null, 0, 0, -0.01, w, h, d - 0.02, 5, w >= 0.85 ? 2 : 1, 'Z', 1);
        } else {
          bodyB.addBox(0, h * 0.5, -0.01, w, h, d - 0.02, 0.75);
          const bottomDrawerH = showDrawers ? h * 0.28 : 0.0;
          const upperH = h - bottomDrawerH - 0.03;
          const upperCenterY = bottomDrawerH + upperH * 0.5 + 0.015;

          if (showDoors) {
            addStyledDoor(bodyB, darkGlassB, metalB, -w * 0.245, upperCenterY, d * 0.5, w * 0.48, upperH, style, showHandles, 1, 'Z', 1);
            addStyledDoor(bodyB, darkGlassB, metalB, w * 0.245, upperCenterY, d * 0.5, w * 0.48, upperH, style, showHandles, -1, 'Z', 1);
          } else {
            addOpenCarcass(bodyB, darkGlassB, 0, bottomDrawerH, -0.01, w, upperH + 0.02, d - 0.02, 3, 1, 'Z', 1);
          }

          if (showDrawers) {
            addStyledDrawerStack(bodyB, darkGlassB, metalB, 0, 0.02, d * 0.5, w * 0.96, bottomDrawerH - 0.02, 2, style, showHandles);
          }
        }
        pushPart(bodyB, 'primary');
        pushPart(darkGlassB, 'dark_screen');
        pushPart(metalB, 'metal');
        break;
      }

      case 'DINING_TABLE': {
        const topH = 0.045;
        const topB = createMeshBuilder();
        const metalB = createMeshBuilder();
        const glassB = createMeshBuilder();
        const isPedestal = style === 'OPEN_SHELVING';
        const isGlass = style === 'GLASS_DISPLAY';

        if (isGlass) {
          glassB.addBox(0, h - topH * 0.5, 0, w, topH * 0.55, d, 0.6);
        } else {
          topB.addBox(0, h - topH * 0.5, 0, w, topH, d, 0.6);
        }

        const legH = h - topH;
        const legTarget = isGlass ? metalB : topB;
        if (isPedestal) {
          legTarget.addCylinder(0, legH * 0.5, 0, Math.min(w, d) * 0.16, legH, 16);
          legTarget.addCylinder(0, 0.02, 0, Math.min(w, d) * 0.36, 0.04, 18);
        } else {
          const lx = w * 0.5 - 0.08;
          const lz = d * 0.5 - 0.08;
          legTarget.addBox(-lx, legH * 0.5, -lz, 0.055, legH, 0.055, 1.0);
          legTarget.addBox(lx, legH * 0.5, -lz, 0.055, legH, 0.055, 1.0);
          legTarget.addBox(-lx, legH * 0.5, lz, 0.055, legH, 0.055, 1.0);
          legTarget.addBox(lx, legH * 0.5, lz, 0.055, legH, 0.055, 1.0);
        }
        addChair(topB, -w * 0.26, -d * 0.5 - 0.12, -1);
        addChair(topB, w * 0.26, -d * 0.5 - 0.12, -1);
        addChair(topB, -w * 0.26, d * 0.5 + 0.12, 1);
        addChair(topB, w * 0.26, d * 0.5 + 0.12, 1);
        pushPart(topB, 'primary');
        pushPart(glassB, 'window_glass');
        pushPart(metalB, 'metal');
        break;
      }

      case 'BED': {
        const frameH = 0.22;
        const legH = 0.12;
        const headH = Math.max(0.95, h);
        const frameB = createMeshBuilder();
        const lx = w * 0.5 - 0.07;
        const lz = d * 0.5 - 0.07;
        frameB.addBox(-lx, legH * 0.5, -lz, 0.06, legH, 0.06, 1.0);
        frameB.addBox(lx, legH * 0.5, -lz, 0.06, legH, 0.06, 1.0);
        frameB.addBox(-lx, legH * 0.5, lz, 0.06, legH, 0.06, 1.0);
        frameB.addBox(lx, legH * 0.5, lz, 0.06, legH, 0.06, 1.0);
        frameB.addBox(0, legH + frameH * 0.5, 0, w, frameH, d, 0.7);
        frameB.addBox(0, headH * 0.5, -d * 0.5 + 0.05, w + 0.06, headH, 0.1, 0.7);
        pushPart(frameB, 'primary');

        const bedB = createMeshBuilder();
        const matH = 0.21;
        const matY = legH + frameH + matH * 0.5;
        bedB.addBox(0, matY, 0.04, w - 0.08, matH, d - 0.14, 0.9);
        const pilY = legH + frameH + matH + 0.055;
        const pw = (w - 0.24) * 0.46;
        bedB.addBox(-w * 0.23, pilY, -d * 0.5 + 0.30, pw, 0.12, 0.36, 1.2);
        bedB.addBox(w * 0.23, pilY, -d * 0.5 + 0.30, pw, 0.12, 0.36, 1.2);
        pushPart(bedB, 'fabric_alt');

        const duvetB = createMeshBuilder();
        duvetB.addBox(0, legH + frameH + matH + 0.025, d * 0.16, w - 0.04, 0.07, d * 0.58, 0.9);
        duvetB.addBox(-w * 0.16, pilY + 0.05, -d * 0.5 + 0.44, pw * 0.75, 0.11, 0.24, 1.1);
        duvetB.addBox(w * 0.16, pilY + 0.05, -d * 0.5 + 0.44, pw * 0.75, 0.11, 0.24, 1.1);
        pushPart(duvetB, 'countertop');
        break;
      }

      case 'WARDROBE': {
        const bodyB = createMeshBuilder();
        const darkGlassB = createMeshBuilder();
        const metalB = createMeshBuilder();
        const doors = w >= 1.5 ? 3 : 2;
        const dw = (w - 0.03) / doors;

        if (!showDoors) {
          // Open walk-in closet with interior shelves + metal hanging rod
          addOpenCarcass(bodyB, null, 0, 0.08, -0.01, w, h - 0.08, d - 0.02, 4, doors, 'Z', 1);
          bodyB.addBox(0, 0.04, 0, w - 0.03, 0.08, d - 0.04, 0.7);
          metalB.addBox(0, h * 0.76, 0.02, w - 0.08, 0.022, 0.022, 1.0);
        } else {
          bodyB.addBox(0, 0.04, 0, w - 0.03, 0.08, d - 0.04, 0.7);
          bodyB.addBox(0, 0.08 + (h - 0.08) * 0.5, -0.01, w, h - 0.08, d - 0.02, 0.7);
          for (let i = 0; i < doors; i++) {
            const dx = -w * 0.5 + 0.015 + dw * (i + 0.5);
            addStyledDoor(bodyB, darkGlassB, metalB, dx, 0.08 + (h - 0.08) * 0.5, d * 0.5, dw - 0.014, h - 0.11, style, showHandles, i === 0 ? 1 : -1, 'Z', 1);
          }
        }
        pushPart(bodyB, 'primary');
        pushPart(darkGlassB, 'dark_screen');
        pushPart(metalB, 'metal');
        break;
      }

      case 'NIGHTSTAND': {
        const legH = 0.12;
        const boxH = Math.max(0.25, h - legH);
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();
        const metalB = createMeshBuilder();
        const lx = w * 0.5 - 0.04, lz = d * 0.5 - 0.04;
        bodyB.addBox(-lx, legH * 0.5, -lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(lx, legH * 0.5, -lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(-lx, legH * 0.5, lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(lx, legH * 0.5, lz, 0.035, legH, 0.035, 1.0);

        if (!showDrawers) {
          addOpenCarcass(bodyB, null, 0, legH, 0, w, boxH, d, 2, 1, 'Z', 1);
        } else {
          bodyB.addBox(0, legH + boxH * 0.5, 0, w, boxH, d, 0.9);
          addStyledDrawerStack(bodyB, darkTrimB, metalB, 0, legH + 0.02, d * 0.5 + 0.008, w - 0.04, boxH - 0.04, 2, style, showHandles);
        }
        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');

        metalB.addCylinder(0, h + 0.07, 0, 0.04, 0.14, 12);
        pushPart(metalB, 'metal');

        const shadeB = createMeshBuilder();
        shadeB.addCylinder(0, h + 0.2, 0, 0.11, 0.15, 14);
        pushPart(shadeB, 'lamp_glow', 0.88);
        break;
      }

      case 'DRESSER': {
        const legH = 0.1;
        const boxH = Math.max(0.4, h - legH);
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();
        const metalB = createMeshBuilder();
        const lx = w * 0.5 - 0.06, lz = d * 0.5 - 0.06;
        bodyB.addBox(-lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(-lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);

        if (!showDrawers) {
          // Open 3x2 cube shelving console instead of drawers
          addOpenCarcass(bodyB, null, 0, legH, 0, w, boxH, d, 3, 2, 'Z', 1);
        } else {
          bodyB.addBox(0, legH + boxH * 0.5, 0, w, boxH, d, 0.7);
          addStyledDrawerStack(bodyB, darkTrimB, metalB, -w * 0.245, legH + 0.02, d * 0.5 + 0.008, w * 0.46, boxH - 0.04, 3, style, showHandles);
          addStyledDrawerStack(bodyB, darkTrimB, metalB, w * 0.245, legH + 0.02, d * 0.5 + 0.008, w * 0.46, boxH - 0.04, 3, style, showHandles);
        }
        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');
        pushPart(metalB, 'metal');
        break;
      }

      case 'SOFA': {
        const legH = 0.12;
        const baseH = 0.16;
        const seatH = 0.16;
        const isChaise = style === 'SHAKER_FRAME' || style === 'GLASS_DISPLAY';
        const isArmless = style === 'OPEN_SHELVING';
        const armW = isArmless ? 0.0 : Math.min(0.18, w * 0.12);
        const backD = Math.min(0.2, d * 0.22);

        const sofaB = createMeshBuilder();
        sofaB.addBox(0, legH + baseH * 0.5, 0, w, baseH, d, 0.85);
        const armH = Math.max(0.35, h * 0.58 - legH);
        if (armW > 0.01) {
          sofaB.addBox(-w * 0.5 + armW * 0.5, legH + baseH + (armH - baseH) * 0.5, 0, armW, armH - baseH, d, 0.85);
          sofaB.addBox(w * 0.5 - armW * 0.5, legH + baseH + (armH - baseH) * 0.5, 0, armW, armH - baseH, d, 0.85);
        }
        const backH = Math.max(0.45, h - legH - baseH);
        sofaB.addBox(0, legH + baseH + backH * 0.5, -d * 0.5 + backD * 0.5, w - armW * 2 + 0.02, backH, backD, 0.85);
        const innerW = w - armW * 2 - 0.02;
        const cw = innerW / 3;
        const seatD = d - backD - 0.02;
        for (let i = 0; i < 3; i++) {
          const cx = -innerW * 0.5 + cw * (i + 0.5);
          const extraLen = (isChaise && i === 2) ? d * 0.55 : 0.0;
          if (extraLen > 0.01) {
            sofaB.addBox(cx, legH + baseH * 0.5, d * 0.5 + extraLen * 0.5, cw, baseH, extraLen, 0.85);
          }
          sofaB.addBox(cx, legH + baseH + seatH * 0.5, backD * 0.5 + extraLen * 0.5, cw - 0.018, seatH, seatD + extraLen, 0.95);
          sofaB.addBox(cx, legH + baseH + seatH + (backH - seatH) * 0.45, -d * 0.5 + backD + 0.07, cw - 0.024, backH - seatH * 0.5, 0.14, 0.95);
        }
        pushPart(sofaB, 'primary');

        const pillowB = createMeshBuilder();
        const py = legH + baseH + seatH + 0.14;
        pillowB.addBox(-innerW * 0.41, py, -d * 0.5 + backD + 0.19, 0.34, 0.30, 0.12, 1.2);
        pillowB.addBox(innerW * 0.41, py, -d * 0.5 + backD + 0.19, 0.34, 0.30, 0.12, 1.2);
        pushPart(pillowB, 'fabric_alt');

        const legsB = createMeshBuilder();
        const lx = w * 0.5 - 0.08, lz = d * 0.5 - 0.08;
        legsB.addCylinder(-lx, legH * 0.5, -lz, 0.024, legH, 8);
        legsB.addCylinder(lx, legH * 0.5, -lz, 0.024, legH, 8);
        legsB.addCylinder(-lx, legH * 0.5, lz, 0.024, legH, 8);
        legsB.addCylinder(lx, legH * 0.5, lz, 0.024, legH, 8);
        pushPart(legsB, 'wood_legs');
        break;
      }

      case 'COFFEE_TABLE': {
        const topH = 0.04;
        const legH = Math.max(0.15, h - topH);
        const topB = createMeshBuilder();
        const glassB = createMeshBuilder();
        const metalB = createMeshBuilder();

        if (style === 'GLASS_DISPLAY') {
          glassB.addBox(0, h - topH * 0.5, 0, w, topH * 0.55, d, 0.7);
          topB.addBox(0, legH * 0.35, 0, w - 0.14, 0.024, d - 0.14, 0.7);
        } else if (style === 'OPEN_SHELVING') {
          topB.addCylinder(0, h - topH * 0.5, 0, Math.min(w, d) * 0.5, topH, 24);
          metalB.addCylinder(0, legH * 0.5, 0, Math.min(w, d) * 0.14, legH, 16);
        } else {
          topB.addBox(0, h - topH * 0.5, 0, w, topH, d, 0.7);
          topB.addBox(0, legH * 0.35, 0, w - 0.14, 0.024, d - 0.14, 0.7);
        }
        pushPart(topB, 'primary');
        pushPart(glassB, 'window_glass');

        const propB = createMeshBuilder();
        propB.addBox(-w * 0.18, h + 0.018, 0.02, 0.28, 0.036, 0.21, 1.0);
        propB.addCylinder(w * 0.16, h + 0.09, -0.04, 0.065, 0.18, 14);
        pushPart(propB, 'fabric_alt');

        if (style !== 'OPEN_SHELVING') {
          const lx = w * 0.5 - 0.06, lz = d * 0.5 - 0.06;
          metalB.addBox(-lx, legH * 0.5, -lz, 0.03, legH, 0.03, 1.0);
          metalB.addBox(lx, legH * 0.5, -lz, 0.03, legH, 0.03, 1.0);
          metalB.addBox(-lx, legH * 0.5, lz, 0.03, legH, 0.03, 1.0);
          metalB.addBox(lx, legH * 0.5, lz, 0.03, legH, 0.03, 1.0);
        }
        pushPart(metalB, 'metal');
        break;
      }

      case 'TV_CONSOLE': {
        const legH = 0.12;
        const cabH = Math.max(0.28, h - legH);
        const bodyB = createMeshBuilder();
        const darkGlassB = createMeshBuilder();
        const metalB = createMeshBuilder();
        const lx = w * 0.5 - 0.08, lz = d * 0.5 - 0.06;
        bodyB.addBox(-lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(-lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);

        if (!showDoors) {
          addOpenCarcass(bodyB, null, 0, legH, 0, w, cabH, d, 2, 3, 'Z', 1);
        } else {
          bodyB.addBox(0, legH + cabH * 0.5, 0, w, cabH, d, 0.75);
          addStyledDoor(bodyB, darkGlassB, metalB, -w * 0.32, legH + cabH * 0.5, d * 0.5 + 0.008, w * 0.31, cabH - 0.04, style, showHandles, 1, 'Z', 1);
          addStyledDoor(bodyB, darkGlassB, metalB, w * 0.32, legH + cabH * 0.5, d * 0.5 + 0.008, w * 0.31, cabH - 0.04, style, showHandles, -1, 'Z', 1);
        }
        pushPart(bodyB, 'primary');
        pushPart(metalB, 'metal');

        const tvW = Math.min(w * 0.78, 1.45);
        const tvH = tvW * 0.56;
        darkGlassB.addBox(0, h + 0.01, 0, 0.36, 0.02, 0.22, 1.0);
        darkGlassB.addBox(0, h + 0.08, -0.02, 0.08, 0.14, 0.04, 1.0);
        darkGlassB.addBox(0, h + 0.12 + tvH * 0.5, 0, tvW, tvH, 0.035, 1.0);
        pushPart(darkGlassB, 'dark_screen');
        break;
      }

      case 'RUG': {
        const rugB = createMeshBuilder();
        rugB.addBox(0, Math.max(0.01, h * 0.5), 0, w, Math.max(0.014, h), d, 1.2);
        pushPart(rugB, 'primary');
        break;
      }

      case 'FLOOR_LAMP': {
        const metalB = createMeshBuilder();
        const poleH = Math.max(1.1, h - 0.28);
        metalB.addCylinder(0, 0.02, 0, Math.min(w, d) * 0.42, 0.04, 18);
        metalB.addCylinder(0, poleH * 0.5, 0, 0.018, poleH, 10);
        pushPart(metalB, 'metal');

        const shadeB = createMeshBuilder();
        shadeB.addCylinder(0, h - 0.16, 0, Math.min(w, d) * 0.46, 0.32, 20);
        pushPart(shadeB, 'lamp_glow', 0.92);
        break;
      }

      default: {
        const b = createMeshBuilder();
        b.addBox(0, h * 0.5, 0, w, h, d, 0.8);
        pushPart(b, 'primary');
      }
    }

    return parts;
  }

  function buildOpeningParts(op, wallHeight, wallThickness) {
    const parts = [];
    const opW = op.widthMeters;
    const isDoor = op.type === 'DOOR';
    const sill = isDoor ? 0 : Math.max(0.15, Math.min(wallHeight - 0.5, op.sillHeightMeters || 0.9));
    const opH = Math.min(wallHeight - sill - 0.12, op.heightMeters || (isDoor ? 2.1 : 1.2));
    const cy = sill + opH * 0.5;
    const frameD = wallThickness + 0.03;

    const frameB = createMeshBuilder();
    const fT = 0.05;
    frameB.addBox(-opW * 0.5 + fT * 0.5, cy, 0, fT, opH, frameD, 1.0);
    frameB.addBox(opW * 0.5 - fT * 0.5, cy, 0, fT, opH, frameD, 1.0);
    frameB.addBox(0, sill + opH - fT * 0.5, 0, opW, fT, frameD, 1.0);
    if (!isDoor) {
      frameB.addBox(0, sill + 0.02, 0.015, opW + 0.06, 0.045, frameD + 0.05, 1.0);
      frameB.addBox(0, cy, 0, 0.045, opH - fT * 2, frameD * 0.55, 1.0);
      frameB.addBox(-opW * 0.25, cy, 0.01, opW * 0.48, 0.03, 0.03, 1.0);
    }
    parts.push({ mesh: frameB.build(), role: isDoor ? 'door_frame' : 'window_frame', emissive: 0.0 });

    if (isDoor) {
      const leafB = createMeshBuilder();
      leafB.addBox(0, cy - fT * 0.25, 0, opW - fT * 2, opH - fT, 0.045, 0.8);
      parts.push({ mesh: leafB.build(), role: 'door_leaf', emissive: 0.0 });

      const handleB = createMeshBuilder();
      handleB.addBox(opW * 0.34, 1.0, 0.045, 0.12, 0.024, 0.04, 1.0);
      handleB.addBox(opW * 0.34, 1.0, -0.045, 0.12, 0.024, 0.04, 1.0);
      parts.push({ mesh: handleB.build(), role: 'metal', emissive: 0.0 });
    } else {
      const backdropB = createMeshBuilder();
      backdropB.addBox(0, cy, -wallThickness * 0.45, opW - fT * 2, opH - fT * 2, 0.01, 1.0);
      parts.push({ mesh: backdropB.build(), role: 'outdoor_backdrop', emissive: 0.62 });

      const glassB = createMeshBuilder();
      glassB.addBox(0, cy, 0.02, opW - fT * 2, opH - fT * 2, 0.016, 1.0);
      parts.push({ mesh: glassB.build(), role: 'window_glass', emissive: 0.22 });
    }

    return parts;
  }

  global.GeometryLib = {
    createMeshBuilder,
    buildWallWithOpenings,
    buildFurnitureParts,
    buildOpeningParts
  };
})(window);
