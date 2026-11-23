// Procedural Architectural Room & Rich Multi-Part Furniture Mesh Builder
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

  function buildFurnitureParts(item) {
    const w = Math.max(0.25, item.widthMeters);
    const d = Math.max(0.25, item.depthMeters);
    const h = Math.max(0.02, item.heightMeters);
    const parts = [];

    function pushPart(builder, role, emissive) {
      parts.push({
        mesh: builder.build(),
        role: role || 'primary',
        emissive: emissive || 0.0
      });
    }

    switch (item.kind) {
      case 'STRAIGHT_COUNTER': {
        const kickH = 0.09;
        const topH = 0.04;
        const golaH = 0.026;
        const bodyH = Math.max(0.3, h - kickH - topH);
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();

        // Dark recessed plinth & Gola handleless shadow channels
        darkTrimB.addBox(0, kickH * 0.5, -0.03, w - 0.04, kickH, d - 0.08, 0.9);
        darkTrimB.addBox(0, h - topH - golaH * 0.5, 0, w, golaH, d - 0.01, 0.9);

        bodyB.addBox(0, kickH + bodyH * 0.5, -0.01, w, bodyH - golaH, d - 0.02, 0.8);
        const doors = Math.max(3, Math.round(w / 0.50));
        const dw = (w - 0.02) / doors;
        for (let i = 0; i < doors; i++) {
          const dx = -w * 0.5 + 0.01 + dw * (i + 0.5);
          if (i === Math.floor(doors / 2) && doors >= 3) {
            // 3-drawer center stack
            const drH = (bodyH - golaH - 0.03) / 3;
            for (let r = 0; r < 3; r++) {
              bodyB.addBox(dx, kickH + drH * (r + 0.5) + 0.01, d * 0.5 - 0.004, dw - 0.012, drH - 0.015, 0.02, 0.8);
            }
            darkTrimB.addBox(dx, kickH + drH * 1.0, d * 0.5 + 0.002, dw - 0.012, 0.014, 0.018, 1.0);
            darkTrimB.addBox(dx, kickH + drH * 2.0, d * 0.5 + 0.002, dw - 0.012, 0.014, 0.018, 1.0);
          } else {
            bodyB.addBox(dx, kickH + (bodyH - golaH) * 0.5, d * 0.5 - 0.004, dw - 0.012, bodyH - golaH - 0.02, 0.02, 0.8);
          }
        }
        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');

        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, 0.008, w + 0.02, topH, d + 0.02, 0.6);
        pushPart(topB, 'countertop');

        if (w >= 1.1) {
          const metalB = createMeshBuilder();
          const sinkX = w >= 2.0 ? -w * 0.28 : 0;
          metalB.addBox(sinkX, h + 0.004, -0.02, 0.56, 0.012, 0.42, 1.0);
          metalB.addCylinder(sinkX, h + 0.13, -d * 0.34, 0.014, 0.26, 10);
          metalB.addBox(sinkX, h + 0.26, -d * 0.26, 0.024, 0.024, 0.16, 1.0);
          pushPart(metalB, 'metal');
        }
        break;
      }

      case 'L_COUNTER': {
        const kickH = 0.09;
        const topH = 0.04;
        const golaH = 0.026;
        const bodyH = Math.max(0.3, h - kickH - topH);
        const legDepth = Math.min(0.64, Math.min(w, d) * 0.42);
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();

        // Back run along X (at -Z) + dark Gola recess + toe-kick
        darkTrimB.addBox(0, kickH * 0.5, -d * 0.5 + legDepth * 0.5 - 0.02, w - 0.04, kickH, legDepth - 0.08, 0.8);
        darkTrimB.addBox(0, h - topH - golaH * 0.5, -d * 0.5 + legDepth * 0.5, w, golaH, legDepth - 0.01, 0.8);
        bodyB.addBox(0, kickH + (bodyH - golaH) * 0.5, -d * 0.5 + legDepth * 0.5, w, bodyH - golaH, legDepth - 0.02, 0.8);

        // Side run along Z (at -X)
        const sideLen = Math.max(0.4, d - legDepth);
        darkTrimB.addBox(-w * 0.5 + legDepth * 0.5 - 0.02, kickH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.08, kickH, sideLen - 0.02, 0.8);
        darkTrimB.addBox(-w * 0.5 + legDepth * 0.5, h - topH - golaH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.01, golaH, sideLen, 0.8);
        bodyB.addBox(-w * 0.5 + legDepth * 0.5, kickH + (bodyH - golaH) * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth - 0.02, bodyH - golaH, sideLen, 0.8);

        // Realistic vertical door gaps & center 3-drawer bank
        const numFront = Math.max(4, Math.round((w - legDepth) / 0.48));
        const dw = (w - legDepth - 0.02) / numFront;
        for (let i = 0; i < numFront; i++) {
          const dx = -w * 0.5 + legDepth + 0.01 + dw * (i + 0.5);
          if (i === Math.floor(numFront / 2)) {
            const drH = (bodyH - golaH - 0.03) / 3;
            for (let r = 0; r < 3; r++) {
              bodyB.addBox(dx, kickH + drH * (r + 0.5) + 0.01, -d * 0.5 + legDepth - 0.002, dw - 0.012, drH - 0.016, 0.02, 0.8);
            }
            darkTrimB.addBox(dx, kickH + drH * 1.0, -d * 0.5 + legDepth + 0.003, dw - 0.012, 0.014, 0.016, 1.0);
            darkTrimB.addBox(dx, kickH + drH * 2.0, -d * 0.5 + legDepth + 0.003, dw - 0.012, 0.014, 0.016, 1.0);
          } else {
            bodyB.addBox(dx, kickH + (bodyH - golaH) * 0.5, -d * 0.5 + legDepth - 0.002, dw - 0.012, bodyH - golaH - 0.02, 0.02, 0.8);
          }
        }
        // Side run doors
        const numSide = Math.max(2, Math.round(sideLen / 0.52));
        const sw = (sideLen - 0.02) / numSide;
        for (let i = 0; i < numSide; i++) {
          const dz = -d * 0.5 + legDepth + 0.01 + sw * (i + 0.5);
          bodyB.addBox(-w * 0.5 + legDepth - 0.002, kickH + (bodyH - golaH) * 0.5, dz, 0.02, bodyH - golaH - 0.02, sw - 0.012, 0.8);
        }

        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');

        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, -d * 0.5 + legDepth * 0.5, w + 0.02, topH, legDepth + 0.02, 0.6);
        topB.addBox(-w * 0.5 + legDepth * 0.5, h - topH * 0.5, -d * 0.5 + legDepth + sideLen * 0.5, legDepth + 0.02, topH, sideLen + 0.01, 0.6);
        pushPart(topB, 'countertop');

        const metalB = createMeshBuilder();
        metalB.addBox(w * 0.24, h + 0.004, -d * 0.5 + legDepth * 0.5, 0.58, 0.012, 0.42, 1.0);
        metalB.addCylinder(w * 0.24, h + 0.13, -d * 0.5 + 0.1, 0.014, 0.26, 10);
        metalB.addBox(w * 0.24, h + 0.26, -d * 0.5 + 0.17, 0.024, 0.024, 0.15, 1.0);
        pushPart(metalB, 'metal');
        break;
      }

      case 'KITCHEN_ISLAND': {
        const kickH = 0.09;
        const topH = 0.045;
        const golaH = 0.024;
        const bodyH = Math.max(0.3, h - kickH - topH);
        const bodyB = createMeshBuilder();
        const darkTrimB = createMeshBuilder();
        darkTrimB.addBox(0, kickH * 0.5, -0.06, w - 0.06, kickH, d - 0.22, 0.8);
        darkTrimB.addBox(0, h - topH - golaH * 0.5, -0.05, w - 0.02, golaH, d - 0.16, 0.8);
        bodyB.addBox(0, kickH + (bodyH - golaH) * 0.5, -0.05, w - 0.02, bodyH - golaH, d - 0.16, 0.8);
        const doors = Math.max(2, Math.round(w / 0.55));
        const dw = (w - 0.06) / doors;
        for (let i = 0; i < doors; i++) {
          const dx = -w * 0.5 + 0.03 + dw * (i + 0.5);
          bodyB.addBox(dx, kickH + (bodyH - golaH) * 0.5, -d * 0.5 + 0.02, dw - 0.015, bodyH - golaH - 0.04, 0.02, 0.8);
        }
        pushPart(bodyB, 'primary');
        pushPart(darkTrimB, 'dark_screen');

        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, 0.02, w + 0.06, topH, d + 0.04, 0.6);
        pushPart(topB, 'countertop');

        const metalB = createMeshBuilder();
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
        // Modern tall upper cabinet with sleek handleless doors, optional dark glass lower strip & open display cubby
        const bodyB = createMeshBuilder();
        const darkGlassB = createMeshBuilder();
        const shelfW = w >= 1.4 ? 0.26 : 0.0;
        const mainW = w - shelfW;
        const mainCenterX = -shelfW * 0.5;

        bodyB.addBox(mainCenterX, h * 0.5, 0, mainW, h, d - 0.02, 0.85);
        const doors = Math.max(2, Math.round(mainW / 0.45));
        const dw = (mainW - 0.02) / doors;
        const hasGlassStrip = h >= 0.75;
        const glassH = hasGlassStrip ? h * 0.28 : 0.0;
        const upperDoorH = h - glassH - 0.02;

        for (let i = 0; i < doors; i++) {
          const dx = mainCenterX - mainW * 0.5 + 0.01 + dw * (i + 0.5);
          bodyB.addBox(dx, glassH + upperDoorH * 0.5 + 0.01, d * 0.5, dw - 0.01, upperDoorH - 0.01, 0.02, 0.85);
          if (hasGlassStrip) {
            darkGlassB.addBox(dx, glassH * 0.5 + 0.005, d * 0.5, dw - 0.01, glassH - 0.01, 0.022, 1.0);
          }
        }
        if (shelfW > 0.1) {
          // Open vertical display shelves on right end
          const sx = w * 0.5 - shelfW * 0.5;
          darkGlassB.addBox(sx, h * 0.5, -d * 0.42, shelfW, h, 0.02, 1.0);
          bodyB.addBox(sx + shelfW * 0.5 - 0.012, h * 0.5, 0, 0.024, h, d, 0.85);
          for (let r = 0; r <= 3; r++) {
            bodyB.addBox(sx, (r / 3) * (h - 0.024) + 0.012, 0, shelfW, 0.022, d, 0.85);
          }
        }
        pushPart(bodyB, 'primary');
        pushPart(darkGlassB, 'dark_screen');

        const ledB = createMeshBuilder();
        ledB.addBox(mainCenterX, 0.004, -d * 0.25, mainW - 0.08, 0.01, 0.028, 1.0);
        pushPart(ledB, 'lamp_glow', 0.98);
        break;
      }

      case 'TALL_PANTRY': {
        const bodyB = createMeshBuilder();
        bodyB.addBox(0, h * 0.5, -0.01, w, h, d - 0.02, 0.75);
        bodyB.addBox(-w * 0.25, h * 0.65, d * 0.5, w * 0.48, h * 0.64, 0.02, 0.75);
        bodyB.addBox(w * 0.25, h * 0.65, d * 0.5, w * 0.48, h * 0.64, 0.02, 0.75);
        bodyB.addBox(0, h * 0.16, d * 0.5, w * 0.96, h * 0.28, 0.02, 0.75);
        pushPart(bodyB, 'primary');

        const metalB = createMeshBuilder();
        metalB.addBox(-w * 0.06, h * 0.58, d * 0.5 + 0.024, 0.018, 0.36, 0.03, 1.0);
        metalB.addBox(w * 0.06, h * 0.58, d * 0.5 + 0.024, 0.018, 0.36, 0.03, 1.0);
        metalB.addBox(0, h * 0.24, d * 0.5 + 0.024, 0.36, 0.018, 0.03, 1.0);
        pushPart(metalB, 'metal');
        break;
      }

      case 'DINING_TABLE': {
        const topH = 0.045;
        const topB = createMeshBuilder();
        topB.addBox(0, h - topH * 0.5, 0, w, topH, d, 0.6);
        const lx = w * 0.5 - 0.08;
        const lz = d * 0.5 - 0.08;
        const legH = h - topH;
        topB.addBox(-lx, legH * 0.5, -lz, 0.055, legH, 0.055, 1.0);
        topB.addBox(lx, legH * 0.5, -lz, 0.055, legH, 0.055, 1.0);
        topB.addBox(-lx, legH * 0.5, lz, 0.055, legH, 0.055, 1.0);
        topB.addBox(lx, legH * 0.5, lz, 0.055, legH, 0.055, 1.0);
        addChair(topB, -w * 0.26, -d * 0.5 - 0.12, -1);
        addChair(topB, w * 0.26, -d * 0.5 - 0.12, -1);
        addChair(topB, -w * 0.26, d * 0.5 + 0.12, 1);
        addChair(topB, w * 0.26, d * 0.5 + 0.12, 1);
        pushPart(topB, 'primary');
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
        bodyB.addBox(0, 0.04, 0, w - 0.03, 0.08, d - 0.04, 0.7);
        bodyB.addBox(0, 0.08 + (h - 0.08) * 0.5, -0.01, w, h - 0.08, d - 0.02, 0.7);
        const doors = w >= 1.5 ? 3 : 2;
        const dw = (w - 0.03) / doors;
        for (let i = 0; i < doors; i++) {
          const dx = -w * 0.5 + 0.015 + dw * (i + 0.5);
          bodyB.addBox(dx, 0.08 + (h - 0.08) * 0.5, d * 0.5, dw - 0.014, h - 0.11, 0.02, 0.7);
        }
        pushPart(bodyB, 'primary');

        const metalB = createMeshBuilder();
        for (let i = 0; i < doors; i++) {
          const dx = -w * 0.5 + 0.015 + dw * (i + 0.5);
          const hx = dx + (i === 0 ? dw * 0.36 : -dw * 0.36);
          metalB.addBox(hx, h * 0.52, d * 0.5 + 0.02, 0.014, 0.36, 0.024, 1.0);
        }
        pushPart(metalB, 'metal');
        break;
      }

      case 'NIGHTSTAND': {
        const legH = 0.12;
        const boxH = Math.max(0.25, h - legH);
        const bodyB = createMeshBuilder();
        const lx = w * 0.5 - 0.04, lz = d * 0.5 - 0.04;
        bodyB.addBox(-lx, legH * 0.5, -lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(lx, legH * 0.5, -lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(-lx, legH * 0.5, lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(lx, legH * 0.5, lz, 0.035, legH, 0.035, 1.0);
        bodyB.addBox(0, legH + boxH * 0.5, 0, w, boxH, d, 0.9);
        bodyB.addBox(0, legH + boxH * 0.27, d * 0.5 + 0.008, w - 0.04, boxH * 0.42, 0.016, 0.9);
        bodyB.addBox(0, legH + boxH * 0.73, d * 0.5 + 0.008, w - 0.04, boxH * 0.42, 0.016, 0.9);
        pushPart(bodyB, 'primary');

        const metalB = createMeshBuilder();
        metalB.addBox(0, legH + boxH * 0.27, d * 0.5 + 0.022, 0.08, 0.014, 0.018, 1.0);
        metalB.addBox(0, legH + boxH * 0.73, d * 0.5 + 0.022, 0.08, 0.014, 0.018, 1.0);
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
        const lx = w * 0.5 - 0.06, lz = d * 0.5 - 0.06;
        bodyB.addBox(-lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(-lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(0, legH + boxH * 0.5, 0, w, boxH, d, 0.7);
        const rows = 3;
        const rh = (boxH - 0.06) / rows;
        for (let r = 0; r < rows; r++) {
          const ry = legH + 0.03 + rh * (r + 0.5);
          bodyB.addBox(-w * 0.245, ry, d * 0.5 + 0.008, w * 0.46, rh - 0.02, 0.018, 0.8);
          bodyB.addBox(w * 0.245, ry, d * 0.5 + 0.008, w * 0.46, rh - 0.02, 0.018, 0.8);
        }
        pushPart(bodyB, 'primary');

        const metalB = createMeshBuilder();
        for (let r = 0; r < rows; r++) {
          const ry = legH + 0.03 + rh * (r + 0.5);
          metalB.addBox(-w * 0.245, ry, d * 0.5 + 0.024, 0.11, 0.014, 0.02, 1.0);
          metalB.addBox(w * 0.245, ry, d * 0.5 + 0.024, 0.11, 0.014, 0.02, 1.0);
        }
        pushPart(metalB, 'metal');
        break;
      }

      case 'SOFA': {
        const legH = 0.12;
        const baseH = 0.16;
        const seatH = 0.16;
        const armW = Math.min(0.18, w * 0.12);
        const backD = Math.min(0.2, d * 0.22);

        const sofaB = createMeshBuilder();
        sofaB.addBox(0, legH + baseH * 0.5, 0, w, baseH, d, 0.85);
        const armH = Math.max(0.35, h * 0.58 - legH);
        sofaB.addBox(-w * 0.5 + armW * 0.5, legH + baseH + (armH - baseH) * 0.5, 0, armW, armH - baseH, d, 0.85);
        sofaB.addBox(w * 0.5 - armW * 0.5, legH + baseH + (armH - baseH) * 0.5, 0, armW, armH - baseH, d, 0.85);
        const backH = Math.max(0.45, h - legH - baseH);
        sofaB.addBox(0, legH + baseH + backH * 0.5, -d * 0.5 + backD * 0.5, w - armW * 2 + 0.02, backH, backD, 0.85);
        const innerW = w - armW * 2 - 0.02;
        const cw = innerW / 3;
        const seatD = d - backD - 0.02;
        for (let i = 0; i < 3; i++) {
          const cx = -innerW * 0.5 + cw * (i + 0.5);
          sofaB.addBox(cx, legH + baseH + seatH * 0.5, backD * 0.5, cw - 0.018, seatH, seatD, 0.95);
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
        topB.addBox(0, h - topH * 0.5, 0, w, topH, d, 0.7);
        topB.addBox(0, legH * 0.35, 0, w - 0.14, 0.024, d - 0.14, 0.7);
        pushPart(topB, 'primary');

        const propB = createMeshBuilder();
        propB.addBox(-w * 0.18, h + 0.018, 0.02, 0.28, 0.036, 0.21, 1.0);
        propB.addCylinder(w * 0.16, h + 0.09, -0.04, 0.065, 0.18, 14);
        pushPart(propB, 'fabric_alt');

        const metalB = createMeshBuilder();
        const lx = w * 0.5 - 0.06, lz = d * 0.5 - 0.06;
        metalB.addBox(-lx, legH * 0.5, -lz, 0.03, legH, 0.03, 1.0);
        metalB.addBox(lx, legH * 0.5, -lz, 0.03, legH, 0.03, 1.0);
        metalB.addBox(-lx, legH * 0.5, lz, 0.03, legH, 0.03, 1.0);
        metalB.addBox(lx, legH * 0.5, lz, 0.03, legH, 0.03, 1.0);
        pushPart(metalB, 'metal');
        break;
      }

      case 'TV_CONSOLE': {
        const legH = 0.12;
        const cabH = Math.max(0.28, h - legH);
        const bodyB = createMeshBuilder();
        const lx = w * 0.5 - 0.08, lz = d * 0.5 - 0.06;
        bodyB.addBox(-lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, -lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(-lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(lx, legH * 0.5, lz, 0.04, legH, 0.04, 1.0);
        bodyB.addBox(0, legH + cabH * 0.5, 0, w, cabH, d, 0.75);
        bodyB.addBox(-w * 0.32, legH + cabH * 0.5, d * 0.5 + 0.008, w * 0.31, cabH - 0.04, 0.016, 0.8);
        bodyB.addBox(w * 0.32, legH + cabH * 0.5, d * 0.5 + 0.008, w * 0.31, cabH - 0.04, 0.016, 0.8);
        pushPart(bodyB, 'primary');

        const tvB = createMeshBuilder();
        const tvW = Math.min(w * 0.78, 1.45);
        const tvH = tvW * 0.56;
        tvB.addBox(0, h + 0.01, 0, 0.36, 0.02, 0.22, 1.0);
        tvB.addBox(0, h + 0.08, -0.02, 0.08, 0.14, 0.04, 1.0);
        tvB.addBox(0, h + 0.12 + tvH * 0.5, 0, tvW, tvH, 0.035, 1.0);
        pushPart(tvB, 'dark_screen');
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
      // Sliding aluminum sash center divider
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
      // Outdoor daylight scenery backdrop behind window + translucent reflective glass
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
