// Procedural PBR Texture & Height-Relief Map Generator (100% Client-Side)
// RGB = Albedo color, Alpha = Surface micro-height (0..255) for real-time bump normals
(function (global) {
  const TextureCache = new Map();

  function hexToRgb(hex) {
    const clean = (hex || '#D8D4CC').replace('#', '');
    const full = clean.length === 3
      ? clean.split('').map(c => c + c).join('')
      : clean.padEnd(6, '0').slice(0, 6);
    const num = parseInt(full, 16);
    return {
      r: (num >> 16) & 255,
      g: (num >> 8) & 255,
      b: num & 255
    };
  }

  function clamp(v, min, max) {
    return Math.max(min, Math.min(max, v));
  }

  function seededRandom(seed) {
    let s = seed % 2147483647;
    if (s <= 0) s += 2147483646;
    return function () {
      s = (s * 16807) % 2147483647;
      return (s - 1) / 2147483646;
    };
  }

  function hash2(x, y) {
    const n = Math.sin(x * 127.1 + y * 311.7) * 43758.5453123;
    return n - Math.floor(n);
  }

  function valueNoise(x, y) {
    const ix = Math.floor(x);
    const iy = Math.floor(y);
    const fx = x - ix;
    const fy = y - iy;
    const ux = fx * fx * (3.0 - 2.0 * fx);
    const uy = fy * fy * (3.0 - 2.0 * fy);
    const a = hash2(ix, iy);
    const b = hash2(ix + 1, iy);
    const c = hash2(ix, iy + 1);
    const d = hash2(ix + 1, iy + 1);
    return a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy;
  }

  function fbm(x, y, octaves) {
    let val = 0;
    let amp = 0.5;
    let freq = 1;
    for (let i = 0; i < octaves; i++) {
      val += amp * valueNoise(x * freq, y * freq);
      freq *= 2.02;
      amp *= 0.5;
    }
    return val;
  }

  function getMaterialProperties(finish) {
    switch (finish) {
      case 'MATTE_PAINT': return { roughness: 0.88, metalness: 0.0, bump: 0.35 };
      case 'SATIN_PAINT': return { roughness: 0.42, metalness: 0.0, bump: 0.22 };
      case 'GLOSSY_PAINT': return { roughness: 0.12, metalness: 0.04, bump: 0.08 };
      case 'WOOD_OAK': return { roughness: 0.48, metalness: 0.0, bump: 0.95 };
      case 'WOOD_WALNUT': return { roughness: 0.38, metalness: 0.0, bump: 0.90 };
      case 'WOOD_BEECH': return { roughness: 0.52, metalness: 0.0, bump: 0.85 };
      case 'MARBLE_WHITE': return { roughness: 0.14, metalness: 0.02, bump: 0.18 };
      case 'QUARTZ_CREAM': return { roughness: 0.18, metalness: 0.02, bump: 0.16 };
      case 'DARK_STONE': return { roughness: 0.28, metalness: 0.05, bump: 0.45 };
      case 'CERAMIC_TILE': return { roughness: 0.15, metalness: 0.03, bump: 1.25 };
      case 'FABRIC_WOVEN': return { roughness: 0.92, metalness: 0.0, bump: 1.25 };
      case 'BRUSHED_METAL': return { roughness: 0.24, metalness: 0.90, bump: 0.45 };
      case 'OUTDOOR_SCENERY': return { roughness: 0.95, metalness: 0.0, bump: 0.0 };
      default: return { roughness: 0.55, metalness: 0.0, bump: 0.3 };
    }
  }

  function createProceduralCanvas(finish, hexColor) {
    const size = 256;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    const base = hexToRgb(hexColor);
    const img = ctx.createImageData(size, size);
    const d = img.data;
    const rand = seededRandom(1337);

    if (finish === 'OUTDOOR_SCENERY') {
      // Soft realistic outdoor daylight sky + stone courtyard + foliage gradient
      const grad = ctx.createLinearGradient(0, 0, 0, size);
      grad.addColorStop(0.0, '#BFD4E6');
      grad.addColorStop(0.45, '#DCE5EC');
      grad.addColorStop(0.60, '#C7BEB2');
      grad.addColorStop(1.0, '#9E9689');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, size, size);
      ctx.fillStyle = 'rgba(110, 128, 98, 0.38)';
      ctx.beginPath();
      ctx.arc(48, 150, 42, 0, Math.PI * 2);
      ctx.arc(215, 162, 50, 0, Math.PI * 2);
      ctx.fill();
      return canvas;
    }

    if (finish === 'MATTE_PAINT' || finish === 'SATIN_PAINT' || finish === 'GLOSSY_PAINT') {
      const grainAmp = finish === 'MATTE_PAINT' ? 6 : finish === 'SATIN_PAINT' ? 4 : 2;
      for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
          const i = (y * size + x) * 4;
          const plaster = (fbm(x * 0.12, y * 0.12, 2) - 0.5) * grainAmp * 1.4;
          const micro = (rand() - 0.5) * grainAmp;
          d[i] = clamp(base.r + plaster + micro, 0, 255);
          d[i + 1] = clamp(base.g + plaster + micro, 0, 255);
          d[i + 2] = clamp(base.b + plaster + micro, 0, 255);
          d[i + 3] = clamp(128 + (plaster + micro) * 5, 0, 255);
        }
      }
      ctx.putImageData(img, 0, 0);
    } else if (finish === 'WOOD_OAK' || finish === 'WOOD_WALNUT' || finish === 'WOOD_BEECH') {
      const plankRows = 4;
      const plankH = size / plankRows;
      for (let y = 0; y < size; y++) {
        const plankIdx = Math.floor(y / plankH);
        const plankShift = (plankIdx * 47) % 100;
        const localY = y % plankH;
        const seamY = localY < 2 || localY > plankH - 2;
        for (let x = 0; x < size; x++) {
          const idx = (y * size + x) * 4;
          const seamX = ((x + plankIdx * 90) % size) < 2;
          const nx = (x + plankShift) * 0.025;
          const ny = y * 0.16;
          const warp = fbm(nx * 0.8, ny * 0.15, 3) * 4.5;
          const ring = Math.sin((ny * 0.55 + warp) * Math.PI);
          const fine = (hash2(x * 0.5, y * 1.9) - 0.5) * 0.12;
          const tone = (plankIdx % 2 === 0 ? 0.025 : -0.025) + ring * 0.12 + fine;
          const isSeam = seamY || seamX;
          const seamMul = isSeam ? 0.68 : 1.0;
          d[idx] = clamp((base.r * (1 + tone)) * seamMul, 0, 255);
          d[idx + 1] = clamp((base.g * (1 + tone * 0.95)) * seamMul, 0, 255);
          d[idx + 2] = clamp((base.b * (1 + tone * 0.88)) * seamMul, 0, 255);
          d[idx + 3] = isSeam ? 28 : clamp(160 + ring * 45 + fine * 90, 60, 255);
        }
      }
      ctx.putImageData(img, 0, 0);
    } else if (finish === 'MARBLE_WHITE' || finish === 'QUARTZ_CREAM' || finish === 'DARK_STONE') {
      const isDark = finish === 'DARK_STONE';
      const isQuartz = finish === 'QUARTZ_CREAM';
      for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
          const idx = (y * size + x) * 4;
          const nx = x / size * 4.0;
          const ny = y / size * 4.0;
          const n = fbm(nx, ny, 4);
          const veinRaw = Math.abs(Math.sin((nx + ny * 0.7 + n * 3.2) * Math.PI));
          const vein = Math.pow(1.0 - veinRaw, isQuartz ? 12.0 : 7.0);
          const fleck = (hash2(x, y) > 0.985 && isQuartz) ? -18 : 0;
          const delta = isDark ? vein * 55 : -vein * 42;
          const cloud = (n - 0.5) * 14;
          d[idx] = clamp(base.r + delta + cloud + fleck, 0, 255);
          d[idx + 1] = clamp(base.g + delta * 0.95 + cloud + fleck, 0, 255);
          d[idx + 2] = clamp(base.b + delta * 0.9 + cloud + fleck, 0, 255);
          d[idx + 3] = clamp(150 - vein * 45 + cloud * 1.5, 40, 255);
        }
      }
      ctx.putImageData(img, 0, 0);
    } else if (finish === 'CERAMIC_TILE') {
      // Staggered running-bond subway ceramic tiles with recessed grout & subtle bevel
      const cols = 3;
      const rows = 8;
      const cw = size / cols;
      const ch = size / rows;
      for (let y = 0; y < size; y++) {
        const row = Math.floor(y / ch);
        const offsetX = (row % 2) * (cw * 0.5);
        const ly = y % ch;
        const gy = ly < 2 || ly > ch - 2;
        for (let x = 0; x < size; x++) {
          const idx = (y * size + x) * 4;
          const tx = (x + offsetX) % size;
          const col = Math.floor(tx / cw);
          const lx = tx % cw;
          const gx = lx < 2 || lx > cw - 2;
          const isGrout = gx || gy;
          const bevel = Math.min(lx, cw - lx, ly, ch - ly);
          if (isGrout) {
            d[idx] = clamp(base.r * 0.72, 0, 255);
            d[idx + 1] = clamp(base.g * 0.72, 0, 255);
            d[idx + 2] = clamp(base.b * 0.72, 0, 255);
            d[idx + 3] = 20;
          } else {
            const tileVar = ((row * 7 + col * 13) % 5 - 2) * 2.5;
            const glaze = (fbm(x * 0.04, y * 0.04, 2) - 0.5) * 6;
            d[idx] = clamp(base.r + tileVar + glaze, 0, 255);
            d[idx + 1] = clamp(base.g + tileVar + glaze, 0, 255);
            d[idx + 2] = clamp(base.b + tileVar + glaze, 0, 255);
            d[idx + 3] = clamp(155 + Math.min(bevel, 5) * 16 + glaze * 2, 40, 255);
          }
        }
      }
      ctx.putImageData(img, 0, 0);
    } else if (finish === 'FABRIC_WOVEN') {
      for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
          const idx = (y * size + x) * 4;
          const warp = Math.sin(x * 1.4) * 7;
          const weft = Math.sin(y * 1.4) * 7;
          const weave = ((x + y) % 4 < 2) ? warp : weft;
          const slub = (fbm(x * 0.08, y * 0.08, 2) - 0.5) * 14;
          d[idx] = clamp(base.r + weave + slub, 0, 255);
          d[idx + 1] = clamp(base.g + weave + slub, 0, 255);
          d[idx + 2] = clamp(base.b + weave + slub, 0, 255);
          d[idx + 3] = clamp(128 + weave * 8 + slub * 3, 10, 255);
        }
      }
      ctx.putImageData(img, 0, 0);
    } else if (finish === 'BRUSHED_METAL') {
      for (let y = 0; y < size; y++) {
        const streak = (hash2(0, y * 0.7) - 0.5) * 20;
        for (let x = 0; x < size; x++) {
          const idx = (y * size + x) * 4;
          const micro = (hash2(x * 0.05, y) - 0.5) * 8;
          d[idx] = clamp(base.r + streak + micro, 0, 255);
          d[idx + 1] = clamp(base.g + streak + micro, 0, 255);
          d[idx + 2] = clamp(base.b + streak + micro, 0, 255);
          d[idx + 3] = clamp(128 + streak * 3 + micro * 2, 20, 255);
        }
      }
      ctx.putImageData(img, 0, 0);
    }
    return canvas;
  }

  function getOrCreateTexture(gl, finish, hexColor) {
    const key = `${finish}_${hexColor}`;
    if (TextureCache.has(key)) {
      return TextureCache.get(key);
    }
    const canvas = createProceduralCanvas(finish, hexColor);
    const tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, canvas);
    gl.generateMipmap(gl.TEXTURE_2D);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.REPEAT);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    const props = getMaterialProperties(finish);
    const entry = {
      texture: tex,
      roughness: props.roughness,
      metalness: props.metalness,
      bump: props.bump
    };
    TextureCache.set(key, entry);
    return entry;
  }

  global.MaterialsLib = {
    hexToRgb,
    getMaterialProperties,
    getOrCreateTexture
  };
})(window);
