// Client-Side WebGL2 Photorealistic PBR Renderer
// Features: Height-Bump Normals, 16-Tap Rotated Poisson Soft Shadows, Depth SSAO,
// Box-Projected Interior Reflections, Volumetric Bloom & ACES Filmic Tone Mapping
(function (global) {
  const Mat4 = {
    identity() {
      return new Float32Array([1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1]);
    },
    multiply(a, b) {
      const out = new Float32Array(16);
      for (let col = 0; col < 4; col++) {
        for (let row = 0; row < 4; row++) {
          out[col * 4 + row] =
            a[0 * 4 + row] * b[col * 4 + 0] +
            a[1 * 4 + row] * b[col * 4 + 1] +
            a[2 * 4 + row] * b[col * 4 + 2] +
            a[3 * 4 + row] * b[col * 4 + 3];
        }
      }
      return out;
    },
    translate(x, y, z) {
      const m = Mat4.identity();
      m[12] = x; m[13] = y; m[14] = z;
      return m;
    },
    rotateY(rad) {
      const c = Math.cos(rad), s = Math.sin(rad);
      return new Float32Array([c,0,-s,0, 0,1,0,0, s,0,c,0, 0,0,0,1]);
    },
    perspective(fovRad, aspect, near, far) {
      const f = 1.0 / Math.tan(fovRad * 0.5);
      const nf = 1.0 / (near - far);
      return new Float32Array([
        f / aspect, 0, 0, 0,
        0, f, 0, 0,
        0, 0, (far + near) * nf, -1,
        0, 0, (2 * far * near) * nf, 0
      ]);
    },
    ortho(left, right, bottom, top, near, far) {
      const lr = 1 / (left - right);
      const bt = 1 / (bottom - top);
      const nf = 1 / (near - far);
      return new Float32Array([
        -2 * lr, 0, 0, 0,
        0, -2 * bt, 0, 0,
        0, 0, 2 * nf, 0,
        (left + right) * lr, (top + bottom) * bt, (far + near) * nf, 1
      ]);
    },
    lookAt(eye, target, up) {
      let zx = eye[0] - target[0], zy = eye[1] - target[1], zz = eye[2] - target[2];
      let len = Math.hypot(zx, zy, zz) || 1;
      zx /= len; zy /= len; zz /= len;
      let xx = up[1] * zz - up[2] * zy;
      let xy = up[2] * zx - up[0] * zz;
      let xz = up[0] * zy - up[1] * zx;
      len = Math.hypot(xx, xy, xz);
      if (len < 1e-5) { xx = 1; xy = 0; xz = 0; } else { xx /= len; xy /= len; xz /= len; }
      const yx = zy * xz - zz * xy;
      const yy = zz * xx - zx * xz;
      const yz = zx * xy - zy * xx;
      return new Float32Array([
        xx, yx, zx, 0,
        xy, yy, zy, 0,
        xz, yz, zz, 0,
        -(xx * eye[0] + xy * eye[1] + xz * eye[2]),
        -(yx * eye[0] + yy * eye[1] + yz * eye[2]),
        -(zx * eye[0] + zy * eye[1] + zz * eye[2]),
        1
      ]);
    },
    invert(m) {
      const inv = new Float32Array(16);
      inv[0] = m[5]*m[10]*m[15] - m[5]*m[11]*m[14] - m[9]*m[6]*m[15] + m[9]*m[7]*m[14] + m[13]*m[6]*m[11] - m[13]*m[7]*m[10];
      inv[4] = -m[4]*m[10]*m[15] + m[4]*m[11]*m[14] + m[8]*m[6]*m[15] - m[8]*m[7]*m[14] - m[12]*m[6]*m[11] + m[12]*m[7]*m[10];
      inv[8] = m[4]*m[9]*m[15] - m[4]*m[11]*m[13] - m[8]*m[5]*m[15] + m[8]*m[7]*m[13] + m[12]*m[5]*m[11] - m[12]*m[7]*m[9];
      inv[12] = -m[4]*m[9]*m[14] + m[4]*m[10]*m[13] + m[8]*m[5]*m[14] - m[8]*m[6]*m[13] - m[12]*m[5]*m[10] + m[12]*m[6]*m[9];
      inv[1] = -m[1]*m[10]*m[15] + m[1]*m[11]*m[14] + m[9]*m[2]*m[15] - m[9]*m[3]*m[14] - m[13]*m[2]*m[11] + m[13]*m[3]*m[10];
      inv[5] = m[0]*m[10]*m[15] - m[0]*m[11]*m[14] - m[8]*m[2]*m[15] + m[8]*m[3]*m[14] + m[12]*m[2]*m[11] - m[12]*m[3]*m[10];
      inv[9] = -m[0]*m[9]*m[15] + m[0]*m[11]*m[13] + m[8]*m[1]*m[15] - m[8]*m[3]*m[13] - m[12]*m[1]*m[11] + m[12]*m[3]*m[9];
      inv[13] = m[0]*m[9]*m[14] - m[0]*m[10]*m[13] - m[8]*m[1]*m[14] + m[8]*m[2]*m[13] + m[12]*m[1]*m[10] - m[12]*m[2]*m[9];
      inv[2] = m[1]*m[6]*m[15] - m[1]*m[7]*m[14] - m[5]*m[2]*m[15] + m[5]*m[3]*m[14] + m[13]*m[2]*m[7] - m[13]*m[3]*m[6];
      inv[6] = -m[0]*m[6]*m[15] + m[0]*m[7]*m[14] + m[4]*m[2]*m[15] - m[4]*m[3]*m[14] - m[12]*m[2]*m[7] + m[12]*m[3]*m[6];
      inv[10] = m[0]*m[5]*m[15] - m[0]*m[7]*m[13] - m[4]*m[1]*m[15] + m[4]*m[3]*m[13] + m[12]*m[1]*m[7] - m[12]*m[3]*m[5];
      inv[14] = -m[0]*m[5]*m[14] + m[0]*m[6]*m[13] + m[4]*m[1]*m[14] - m[4]*m[2]*m[13] - m[12]*m[1]*m[6] + m[12]*m[2]*m[5];
      inv[3] = -m[1]*m[6]*m[11] + m[1]*m[7]*m[10] + m[5]*m[2]*m[11] - m[5]*m[3]*m[10] - m[9]*m[2]*m[7] + m[9]*m[3]*m[6];
      inv[7] = m[0]*m[6]*m[11] - m[0]*m[7]*m[10] - m[4]*m[2]*m[11] + m[4]*m[3]*m[10] + m[8]*m[2]*m[7] - m[8]*m[3]*m[6];
      inv[11] = -m[0]*m[5]*m[11] + m[0]*m[7]*m[9] + m[4]*m[1]*m[11] - m[4]*m[3]*m[9] - m[8]*m[1]*m[7] + m[8]*m[3]*m[5];
      inv[15] = m[0]*m[5]*m[10] - m[0]*m[6]*m[9] - m[4]*m[1]*m[10] + m[4]*m[2]*m[9] + m[8]*m[1]*m[6] - m[8]*m[2]*m[5];
      let det = m[0]*inv[0] + m[1]*inv[4] + m[2]*inv[8] + m[3]*inv[12];
      if (Math.abs(det) < 1e-8) return Mat4.identity();
      det = 1.0 / det;
      for (let i = 0; i < 16; i++) inv[i] *= det;
      return inv;
    }
  };

  const SHADOW_VS = `#version 300 es
  precision highp float;
  layout(location = 0) in vec3 aPosition;
  uniform mat4 uLightVP;
  uniform mat4 uModel;
  void main() {
    gl_Position = uLightVP * uModel * vec4(aPosition, 1.0);
  }`;

  const SHADOW_FS = `#version 300 es
  precision highp float;
  out vec4 fragColor;
  void main() {
    fragColor = vec4(1.0);
  }`;

  const SCENE_VS = `#version 300 es
  precision highp float;
  layout(location = 0) in vec3 aPosition;
  layout(location = 1) in vec3 aNormal;
  layout(location = 2) in vec2 aUv;

  uniform mat4 uVP;
  uniform mat4 uModel;
  uniform mat4 uLightVP;

  out vec3 vWorldPos;
  out vec3 vNormal;
  out vec2 vUv;
  out vec4 vLightPos;

  void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorldPos = world.xyz;
    vNormal = normalize(mat3(uModel) * aNormal);
    vUv = aUv;
    vLightPos = uLightVP * world;
    gl_Position = uVP * world;
  }`;

  const SCENE_FS = `#version 300 es
  precision highp float;

  in vec3 vWorldPos;
  in vec3 vNormal;
  in vec2 vUv;
  in vec4 vLightPos;

  uniform sampler2D uMap;
  uniform sampler2D uShadowMap;
  uniform vec3 uCameraPos;
  uniform vec3 uSunDir;
  uniform vec3 uSunColor;
  uniform vec3 uSkyColor;
  uniform vec3 uGroundColor;
  uniform float uRoughness;
  uniform float uMetalness;
  uniform float uBump;
  uniform float uEmissive;
  uniform float uOpacity;
  uniform float uRealistic;
  uniform float uExposure;
  uniform float uSelected;
  uniform float uIsFloor;
  uniform vec3 uRoomBounds; // W, H, L
  uniform float uTime;

  uniform int uNumLights;
  uniform vec4 uPointLights[6]; // xyz = pos, w = intensity
  uniform vec3 uPointColor;

  out vec4 fragColor;

  const float PI = 3.14159265359;

  float interleavedGradientNoise(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
  }

  vec3 perturbNormal(vec3 N, vec3 pos, vec2 uv, float strength) {
    float hC = texture(uMap, uv).a;
    float hX = texture(uMap, uv + vec2(0.0038, 0.0)).a;
    float hY = texture(uMap, uv + vec2(0.0, 0.0038)).a;
    vec3 dp1 = dFdx(pos);
    vec3 dp2 = dFdy(pos);
    vec2 duv1 = dFdx(uv);
    vec2 duv2 = dFdy(uv);
    vec3 dp2perp = cross(dp2, N);
    vec3 dp1perp = cross(N, dp1);
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
    float invmax = inversesqrt(max(dot(T, T), dot(B, B)) + 1e-6);
    float dHx = (hX - hC) * strength * 4.2;
    float dHy = (hY - hC) * strength * 4.2;
    return normalize(N - (T * dHx + B * dHy) * invmax);
  }

  float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float denom = (NdotH * NdotH * (a2 - 1.0) + 1.0);
    return a2 / max(PI * denom * denom, 0.0001);
  }

  float geometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / max(NdotV * (1.0 - k) + k, 0.0001);
  }

  float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    return geometrySchlickGGX(max(dot(N, V), 0.0), roughness) *
           geometrySchlickGGX(max(dot(N, L), 0.0), roughness);
  }

  vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
  }

  // Box-projected indoor studio environment reflection for floors, marble, tiles & metal
  vec3 sampleRoomEnv(vec3 pos, vec3 R, float roughness) {
    vec3 halfB = vec3(uRoomBounds.x * 0.5, uRoomBounds.y, uRoomBounds.z * 0.5);
    vec3 invR = 1.0 / (R + sign(R) * 1e-4);
    vec3 tMax = (halfB - pos) * invR;
    vec3 tMin = (vec3(-halfB.x, 0.0, -halfB.z) - pos) * invR;
    vec3 t1 = max(tMin, tMax);
    float tHit = min(min(t1.x, t1.y), t1.z);
    vec3 hitPos = pos + R * max(tHit, 0.05);

    vec3 sharp;
    if (hitPos.y >= uRoomBounds.y - 0.06) {
      // Ceiling + recessed warm ceiling spotlights
      vec2 grid = abs(fract(hitPos.xz / (uRoomBounds.xz * 0.45) + 0.5) - 0.5);
      float spot = 1.0 - smoothstep(0.06, 0.16, length(grid));
      sharp = uSkyColor * 0.72 + vec3(1.8, 1.45, 1.05) * spot;
    } else if (hitPos.y <= 0.05) {
      sharp = uGroundColor * 0.65;
    } else {
      // Walls + sunlit window reflection band
      float winBand = smoothstep(0.7, 1.0, R.y + 0.35) * smoothstep(0.2, 0.9, dot(normalize(R.xz + 1e-4), normalize(uSunDir.xz + 1e-4)));
      sharp = mix(uGroundColor * 0.85, uSkyColor * 0.92, clamp(hitPos.y / max(uRoomBounds.y, 1.0), 0.0, 1.0));
      sharp += uSunColor * winBand * 0.95;
    }

    vec3 blurred = mix(uGroundColor * 0.72, uSkyColor * 0.96, R.y * 0.5 + 0.5) + uSunColor * 0.15;
    return mix(sharp, blurred, clamp(roughness * 0.92, 0.0, 1.0));
  }

  float computeShadow(vec4 lightPos, vec3 N, vec3 L) {
    vec3 proj = lightPos.xyz / lightPos.w;
    proj = proj * 0.5 + 0.5;
    if (proj.z > 1.0 || proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) {
      return 1.0;
    }
    float bias = max(0.0022 * (1.0 - dot(N, L)), 0.0007);
    float angle = interleavedGradientNoise(gl_FragCoord.xy) * 6.2831853;
    float s = sin(angle), c = cos(angle);
    mat2 rot = mat2(c, -s, s, c);
    vec2 spread = vec2(2.6 / 2048.0);

    vec2 poisson[16];
    poisson[0] = vec2(-0.9420, -0.3990); poisson[1] = vec2(0.9456, -0.7689);
    poisson[2] = vec2(-0.0942, -0.9293); poisson[3] = vec2(0.3449, 0.2938);
    poisson[4] = vec2(-0.9158, 0.4577);  poisson[5] = vec2(-0.8154, -0.8791);
    poisson[6] = vec2(-0.3827, 0.2767);  poisson[7] = vec2(0.9748, 0.7564);
    poisson[8] = vec2(0.4432, -0.9751);  poisson[9] = vec2(0.5374, -0.4737);
    poisson[10] = vec2(-0.2649, -0.4189); poisson[11] = vec2(0.7919, 0.1909);
    poisson[12] = vec2(-0.2418, 0.9970); poisson[13] = vec2(-0.8140, 0.9143);
    poisson[14] = vec2(0.1998, 0.7864);  poisson[15] = vec2(0.1438, -0.1410);

    float shadow = 0.0;
    for (int i = 0; i < 16; i++) {
      vec2 offset = (rot * poisson[i]) * spread;
      float pcfDepth = texture(uShadowMap, proj.xy + offset).r;
      shadow += (proj.z - bias) > pcfDepth ? 0.0 : 1.0;
    }
    return shadow / 16.0;
  }

  void main() {
    vec3 texSample = texture(uMap, vUv).rgb;
    vec3 albedo = pow(texSample, vec3(2.2));
    vec3 geomN = normalize(vNormal);
    vec3 N = (uRealistic > 0.5 && uBump > 0.01)
      ? perturbNormal(geomN, vWorldPos, vUv, uBump)
      : geomN;
    vec3 V = normalize(uCameraPos - vWorldPos);

    // Architectural corner/floor contact ambient occlusion
    float floorAO = mix(0.66, 1.0, smoothstep(0.0, 0.26, vWorldPos.y));
    float ceilAO = mix(0.76, 1.0, smoothstep(0.0, 0.22, max(0.0, uRoomBounds.y - vWorldPos.y)));
    float wallDistX = max(0.0, uRoomBounds.x * 0.5 - abs(vWorldPos.x));
    float wallDistZ = max(0.0, uRoomBounds.z * 0.5 - abs(vWorldPos.z));
    float wallAO = mix(0.74, 1.0, smoothstep(0.0, 0.30, min(wallDistX, wallDistZ)));
    float ao = mix(1.0, min(min(floorAO, ceilAO), wallAO), uIsFloor > 0.5 ? 0.85 : 0.48);

    vec3 color = vec3(0.0);

    if (uRealistic > 0.5) {
      vec3 F0 = mix(vec3(0.04), albedo, uMetalness);
      vec3 L = normalize(uSunDir);
      vec3 H = normalize(V + L);

      float shadow = computeShadow(vLightPos, geomN, L);
      float NDF = distributionGGX(N, H, max(uRoughness, 0.05));
      float G = geometrySmith(N, V, L, max(uRoughness, 0.05));
      vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

      vec3 spec = (NDF * G * F) / max(4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0), 0.001);
      vec3 kD = (vec3(1.0) - F) * (1.0 - uMetalness);
      float NdotL = max(dot(N, L), 0.0);
      vec3 direct = (kD * albedo / PI + spec) * uSunColor * NdotL * (0.20 + 0.80 * shadow);

      // Multi-bounce warm indoor GI & point lights / spotlights
      vec3 indoor = vec3(0.0);
      for (int i = 0; i < 6; i++) {
        if (i >= uNumLights) break;
        vec3 lPos = uPointLights[i].xyz;
        float lInt = uPointLights[i].w;
        vec3 pVec = lPos - vWorldPos;
        float dist = length(pVec);
        vec3 pL = pVec / max(dist, 0.001);
        vec3 pH = normalize(V + pL);
        float atten = lInt / (1.0 + 0.32 * dist + 0.20 * dist * dist);
        float pNdotL = max(dot(N, pL), 0.0);
        float pNDF = distributionGGX(N, pH, max(uRoughness, 0.10));
        vec3 pF = fresnelSchlick(max(dot(pH, V), 0.0), F0);
        vec3 pSpec = pNDF * pF * 0.28;
        indoor += ((vec3(1.0) - pF) * (1.0 - uMetalness) * albedo / PI + pSpec) * uPointColor * pNdotL * atten;
      }

      // Hemisphere Indirect GI + Box-Projected Specular Reflections
      float hemi = N.y * 0.5 + 0.5;
      vec3 bounce = mix(uGroundColor, uSkyColor, hemi);
      vec3 ambientDiff = bounce * albedo * (1.0 - uMetalness) * 0.48;
      vec3 R = reflect(-V, N);
      vec3 envColor = sampleRoomEnv(vWorldPos, R, uRoughness);
      vec3 envF = fresnelSchlick(max(dot(N, V), 0.0), F0);
      float envStrength = mix(0.32, 0.96, uMetalness) * (1.0 - uRoughness * 0.72);
      vec3 ambientSpec = envColor * envF * envStrength;

      color = (direct + indoor + (ambientDiff + ambientSpec) * ao) * uExposure;
    } else {
      // Clean Studio / Floorplan 3D shading
      vec3 L1 = normalize(vec3(0.55, 0.85, 0.45));
      vec3 L2 = normalize(vec3(-0.6, 0.45, -0.5));
      float d1 = max(dot(N, L1), 0.0) * 0.62;
      float d2 = max(dot(N, L2), 0.0) * 0.24;
      float hemi = (N.y * 0.5 + 0.5) * 0.36 + 0.22;
      vec3 H = normalize(V + L1);
      float specPow = mix(8.0, 64.0, 1.0 - uRoughness);
      float spec = pow(max(dot(N, H), 0.0), specPow) * (1.0 - uRoughness * 0.75) * 0.28;
      color = albedo * (d1 + d2 + hemi) * ao + vec3(spec);

      if (uIsFloor > 0.5 && uRealistic < 0.5) {
        vec2 g1 = abs(fract(vWorldPos.xz - 0.5) - 0.5) / max(fwidth(vWorldPos.xz), vec2(0.002));
        float line1 = 1.0 - min(min(g1.x, g1.y), 1.0);
        vec2 g2 = abs(fract(vWorldPos.xz * 2.0 - 0.5) - 0.5) / max(fwidth(vWorldPos.xz * 2.0), vec2(0.002));
        float line2 = 1.0 - min(min(g2.x, g2.y), 1.0);
        color = mix(color, vec3(0.18, 0.22, 0.28), line2 * 0.08 + line1 * 0.16);
      }
    }

    if (uEmissive > 0.01) {
      vec3 emCol = uEmissive > 0.87 ? vec3(1.0, 0.88, 0.66) : mix(uSkyColor * 1.25, uSunColor * 1.15, 0.4);
      color += emCol * uEmissive * (uRealistic > 0.5 ? 1.25 : 0.45);
    }

    if (uSelected > 0.5) {
      float rim = pow(1.0 - max(dot(N, V), 0.0), 2.2);
      float pulse = 0.7 + 0.3 * sin(uTime * 4.0);
      vec3 selCol = vec3(0.92, 0.42, 0.24);
      color = mix(color, selCol, 0.14 + rim * 0.42 * pulse);
    }

    color = color / (1.0 + color * 0.28);
    fragColor = vec4(color, uOpacity);
  }`;

  const QUAD_VS = `#version 300 es
  precision highp float;
  layout(location = 0) in vec2 aPos;
  out vec2 vUv;
  void main() {
    vUv = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
  }`;

  // Screen-Space Ambient Occlusion (SSAO) from scene depth texture
  const SSAO_FS = `#version 300 es
  precision highp float;
  in vec2 vUv;
  uniform sampler2D uDepth;
  uniform mat4 uInvProj;
  uniform mat4 uProj;
  out vec4 fragColor;

  vec3 viewPosFromDepth(vec2 uv) {
    float d = texture(uDepth, uv).r;
    vec4 clip = vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    vec4 view = uInvProj * clip;
    return view.xyz / max(view.w, 1e-5);
  }

  float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
  }

  void main() {
    float rawD = texture(uDepth, vUv).r;
    if (rawD >= 0.9995) {
      fragColor = vec4(1.0);
      return;
    }

    vec3 pos = viewPosFromDepth(vUv);
    vec2 texel = 1.0 / vec2(textureSize(uDepth, 0));
    vec3 pL = viewPosFromDepth(vUv - vec2(texel.x, 0.0));
    vec3 pR = viewPosFromDepth(vUv + vec2(texel.x, 0.0));
    vec3 pD = viewPosFromDepth(vUv - vec2(0.0, texel.y));
    vec3 pU = viewPosFromDepth(vUv + vec2(0.0, texel.y));
    vec3 dx = abs(pR.z - pos.z) < abs(pos.z - pL.z) ? (pR - pos) : (pos - pL);
    vec3 dy = abs(pU.z - pos.z) < abs(pos.z - pD.z) ? (pU - pos) : (pos - pD);
    vec3 N = normalize(cross(dx, dy));

    float randAng = hash12(gl_FragCoord.xy) * 6.2831853;
    float radius = 0.42;
    float occ = 0.0;

    for (int i = 0; i < 12; i++) {
      float fi = float(i) + 0.5;
      float r = sqrt(fi / 12.0) * radius;
      float a = fi * 2.399963 + randAng;
      vec3 dir = vec3(cos(a) * r, sin(a) * r, (hash12(gl_FragCoord.xy + fi) * 0.65 + 0.2) * r);
      if (dot(dir, N) < 0.0) dir = -dir;
      vec3 samplePos = pos + dir;
      vec4 proj = uProj * vec4(samplePos, 1.0);
      vec2 sUv = (proj.xy / proj.w) * 0.5 + 0.5;
      if (sUv.x > 0.0 && sUv.x < 1.0 && sUv.y > 0.0 && sUv.y < 1.0) {
        vec3 realPos = viewPosFromDepth(sUv);
        float rangeCheck = smoothstep(0.0, 1.0, radius / max(abs(pos.z - realPos.z), 0.001));
        occ += (realPos.z >= samplePos.z + 0.025 ? 1.0 : 0.0) * rangeCheck;
      }
    }

    float ao = 1.0 - (occ / 12.0) * 0.72;
    fragColor = vec4(vec3(clamp(ao, 0.25, 1.0)), 1.0);
  }`;

  const BRIGHT_FS = `#version 300 es
  precision highp float;
  in vec2 vUv;
  uniform sampler2D uTex;
  out vec4 fragColor;
  void main() {
    vec3 c = texture(uTex, vUv).rgb;
    float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
    float w = smoothstep(0.64, 0.94, lum);
    fragColor = vec4(c * w, 1.0);
  }`;

  const BLUR_FS = `#version 300 es
  precision highp float;
  in vec2 vUv;
  uniform sampler2D uTex;
  uniform vec2 uDir;
  out vec4 fragColor;
  void main() {
    vec3 result = texture(uTex, vUv).rgb * 0.227027;
    vec2 stepUv = uDir;
    result += texture(uTex, vUv + stepUv * 1.3846).rgb * 0.316216;
    result += texture(uTex, vUv - stepUv * 1.3846).rgb * 0.316216;
    result += texture(uTex, vUv + stepUv * 3.2307).rgb * 0.070270;
    result += texture(uTex, vUv - stepUv * 3.2307).rgb * 0.070270;
    fragColor = vec4(result, 1.0);
  }`;

  const COMPOSITE_FS = `#version 300 es
  precision highp float;
  in vec2 vUv;
  uniform sampler2D uScene;
  uniform sampler2D uBloom;
  uniform sampler2D uSsao;
  uniform float uRealistic;
  out vec4 fragColor;

  vec3 acesToneMap(vec3 x) {
    float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
  }

  void main() {
    vec3 col = texture(uScene, vUv).rgb;
    col = col / max(1.0 - col * 0.28, vec3(0.08));

    if (uRealistic > 0.5) {
      float ssao = texture(uSsao, vUv).r;
      col *= mix(1.0, ssao, 0.52);
      vec3 bloom = texture(uBloom, vUv).rgb;
      col += bloom * 0.42;
      col = acesToneMap(col * 1.08);
      float dist = distance(vUv, vec2(0.5));
      col *= mix(1.0, 0.88, smoothstep(0.46, 0.94, dist));
    } else {
      col = col / (1.0 + col * 0.15);
    }

    col = pow(clamp(col, 0.0, 1.0), vec3(1.0 / 2.2));
    fragColor = vec4(col, 1.0);
  }`;

  function compileShader(gl, type, src) {
    const sh = gl.createShader(type);
    gl.shaderSource(sh, src);
    gl.compileShader(sh);
    if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
      console.error('Shader error:', gl.getShaderInfoLog(sh));
    }
    return sh;
  }

  function createProgram(gl, vsSrc, fsSrc) {
    const prog = gl.createProgram();
    gl.attachShader(prog, compileShader(gl, gl.VERTEX_SHADER, vsSrc));
    gl.attachShader(prog, compileShader(gl, gl.FRAGMENT_SHADER, fsSrc));
    gl.linkProgram(prog);
    if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
      console.error('Program link error:', gl.getProgramInfoLog(prog));
    }
    return prog;
  }

  function createBufferMesh(gl, data) {
    const vao = gl.createVertexArray();
    gl.bindVertexArray(vao);

    const vboPos = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, vboPos);
    gl.bufferData(gl.ARRAY_BUFFER, data.positions, gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

    const vboNorm = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, vboNorm);
    gl.bufferData(gl.ARRAY_BUFFER, data.normals, gl.STATIC_DRAW);
    gl.enableVertexAttribArray(1);
    gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

    const vboUv = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, vboUv);
    gl.bufferData(gl.ARRAY_BUFFER, data.uvs, gl.STATIC_DRAW);
    gl.enableVertexAttribArray(2);
    gl.vertexAttribPointer(2, 2, gl.FLOAT, false, 0, 0);

    const ibo = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, ibo);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, data.indices, gl.STATIC_DRAW);

    gl.bindVertexArray(null);
    return { vao, count: data.indices.length, buffers: [vboPos, vboNorm, vboUv, ibo] };
  }

  function deleteBufferMesh(gl, m) {
    if (!m) return;
    gl.deleteVertexArray(m.vao);
    for (const b of m.buffers) gl.deleteBuffer(b);
  }

  function createSceneTargetFbo(gl, w, h) {
    const tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

    const depthTex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, depthTex);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.DEPTH_COMPONENT24, w, h, 0, gl.DEPTH_COMPONENT, gl.UNSIGNED_INT, null);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

    const fbo = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.TEXTURE_2D, depthTex, 0);
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return { fbo, tex, depthTex, w, h };
  }

  function createHalfFbo(gl, w, h) {
    const tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

    const fbo = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return { fbo, tex, w, h };
  }

  function createRenderer(canvas) {
    const gl = canvas.getContext('webgl2', {
      antialias: true,
      alpha: false,
      preserveDrawingBuffer: true
    });
    if (!gl) return null;

    const shadowProg = createProgram(gl, SHADOW_VS, SHADOW_FS);
    const sceneProg = createProgram(gl, SCENE_VS, SCENE_FS);
    const ssaoProg = createProgram(gl, QUAD_VS, SSAO_FS);
    const brightProg = createProgram(gl, QUAD_VS, BRIGHT_FS);
    const blurProg = createProgram(gl, QUAD_VS, BLUR_FS);
    const compProg = createProgram(gl, QUAD_VS, COMPOSITE_FS);

    const quadVao = gl.createVertexArray();
    gl.bindVertexArray(quadVao);
    const quadVbo = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, quadVbo);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1, 1,-1, -1,1, 1,1]), gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    gl.bindVertexArray(null);

    const shadowSize = 2048;
    const shadowTex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, shadowTex);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.DEPTH_COMPONENT24, shadowSize, shadowSize, 0, gl.DEPTH_COMPONENT, gl.UNSIGNED_INT, null);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

    const shadowFbo = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, shadowFbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.TEXTURE_2D, shadowTex, 0);
    gl.drawBuffers([gl.NONE]);
    gl.readBuffer(gl.NONE);
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);

    let sceneTarget = null;
    let bloomA = null;
    let bloomB = null;
    let ssaoA = null;
    let ssaoB = null;

    function ensureRenderTargets(w, h) {
      if (sceneTarget && sceneTarget.w === w && sceneTarget.h === h) return;
      if (sceneTarget) {
        gl.deleteFramebuffer(sceneTarget.fbo);
        gl.deleteTexture(sceneTarget.tex);
        gl.deleteTexture(sceneTarget.depthTex);
      }
      if (bloomA) { gl.deleteFramebuffer(bloomA.fbo); gl.deleteTexture(bloomA.tex); }
      if (bloomB) { gl.deleteFramebuffer(bloomB.fbo); gl.deleteTexture(bloomB.tex); }
      if (ssaoA) { gl.deleteFramebuffer(ssaoA.fbo); gl.deleteTexture(ssaoA.tex); }
      if (ssaoB) { gl.deleteFramebuffer(ssaoB.fbo); gl.deleteTexture(ssaoB.tex); }
      sceneTarget = createSceneTargetFbo(gl, w, h);
      const bw = Math.max(2, Math.floor(w / 2));
      const bh = Math.max(2, Math.floor(h / 2));
      bloomA = createHalfFbo(gl, bw, bh);
      bloomB = createHalfFbo(gl, bw, bh);
      ssaoA = createHalfFbo(gl, bw, bh);
      ssaoB = createHalfFbo(gl, bw, bh);
    }

    function getLightingConfig(design) {
      const az = ((design.sunAzimuthDeg ?? 42) * Math.PI) / 180;
      const preset = design.lightingPreset || 'DAYLIGHT';
      if (preset === 'GOLDEN_HOUR') {
        return {
          sunDir: [Math.cos(az) * 0.9, 0.48, Math.sin(az) * 0.9],
          sunColor: [1.65, 1.08, 0.66],
          skyColor: [0.96, 0.84, 0.72],
          groundColor: [0.46, 0.36, 0.28],
          pointColor: [1.45, 0.98, 0.56],
          bg: [0.16, 0.14, 0.13]
        };
      } else if (preset === 'INDOOR_SPOTLIGHTS') {
        return {
          sunDir: [Math.cos(az) * 0.55, 0.92, Math.sin(az) * 0.55],
          sunColor: [0.62, 0.70, 0.92],
          skyColor: [0.54, 0.60, 0.76],
          groundColor: [0.34, 0.30, 0.26],
          pointColor: [1.95, 1.38, 0.85],
          bg: [0.09, 0.10, 0.13]
        };
      }
      return {
        sunDir: [Math.cos(az) * 0.75, 0.82, Math.sin(az) * 0.75],
        sunColor: [1.52, 1.44, 1.28],
        skyColor: [0.88, 0.93, 1.04],
        groundColor: [0.54, 0.50, 0.46],
        pointColor: [1.22, 0.92, 0.60],
        bg: design.realisticView ? [0.14, 0.16, 0.19] : [0.93, 0.92, 0.90]
      };
    }

    function render(renderList, cameraPos, projMatrix, viewMatrix, design, pointLights, timeSec) {
      const w = canvas.width;
      const h = canvas.height;
      if (w < 2 || h < 2) return;
      ensureRenderTargets(w, h);

      const vpMatrix = Mat4.multiply(projMatrix, viewMatrix);
      const invProjMatrix = Mat4.invert(projMatrix);
      const lightCfg = getLightingConfig(design);
      const realistic = design.realisticView ? 1.0 : 0.0;
      const roomSpan = Math.max(design.widthMeters, design.lengthMeters) * 0.85 + 2.0;
      const sd = lightCfg.sunDir;
      const sLen = Math.hypot(sd[0], sd[1], sd[2]) || 1;
      const sunEye = [(sd[0] / sLen) * 10, (sd[1] / sLen) * 10, (sd[2] / sLen) * 10];
      const lightV = Mat4.lookAt(sunEye, [0, design.wallHeightMeters * 0.35, 0], [0, 1, 0]);
      const lightP = Mat4.ortho(-roomSpan, roomSpan, -roomSpan, roomSpan, 1.0, 26.0);
      const lightVP = Mat4.multiply(lightP, lightV);

      gl.enable(gl.DEPTH_TEST);
      gl.enable(gl.CULL_FACE);

      // Pass 1: 2048x2048 Shadow Map
      if (realistic > 0.5) {
        gl.bindFramebuffer(gl.FRAMEBUFFER, shadowFbo);
        gl.viewport(0, 0, shadowSize, shadowSize);
        gl.clear(gl.DEPTH_BUFFER_BIT);
        gl.cullFace(gl.FRONT);
        gl.useProgram(shadowProg);
        gl.uniformMatrix4fv(gl.getUniformLocation(shadowProg, 'uLightVP'), false, lightVP);
        const uModelLoc = gl.getUniformLocation(shadowProg, 'uModel');
        for (const item of renderList) {
          if (!item.castShadow) continue;
          gl.uniformMatrix4fv(uModelLoc, false, item.modelMatrix);
          gl.bindVertexArray(item.gpuMesh.vao);
          gl.drawElements(gl.TRIANGLES, item.gpuMesh.count, gl.UNSIGNED_SHORT, 0);
        }
        gl.cullFace(gl.BACK);
      }

      // Pass 2: Main Scene + Depth Texture
      gl.bindFramebuffer(gl.FRAMEBUFFER, sceneTarget.fbo);
      gl.viewport(0, 0, w, h);
      gl.clearColor(lightCfg.bg[0], lightCfg.bg[1], lightCfg.bg[2], 1.0);
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

      gl.useProgram(sceneProg);
      gl.uniformMatrix4fv(gl.getUniformLocation(sceneProg, 'uVP'), false, vpMatrix);
      gl.uniformMatrix4fv(gl.getUniformLocation(sceneProg, 'uLightVP'), false, lightVP);
      gl.uniform3fv(gl.getUniformLocation(sceneProg, 'uCameraPos'), cameraPos);
      gl.uniform3fv(gl.getUniformLocation(sceneProg, 'uSunDir'), sd);
      gl.uniform3fv(gl.getUniformLocation(sceneProg, 'uSunColor'), lightCfg.sunColor);
      gl.uniform3fv(gl.getUniformLocation(sceneProg, 'uSkyColor'), lightCfg.skyColor);
      gl.uniform3fv(gl.getUniformLocation(sceneProg, 'uGroundColor'), lightCfg.groundColor);
      gl.uniform3fv(gl.getUniformLocation(sceneProg, 'uPointColor'), lightCfg.pointColor);
      gl.uniform1f(gl.getUniformLocation(sceneProg, 'uRealistic'), realistic);
      gl.uniform1f(gl.getUniformLocation(sceneProg, 'uExposure'), design.exposure || 1.08);
      gl.uniform3f(gl.getUniformLocation(sceneProg, 'uRoomBounds'), design.widthMeters, design.wallHeightMeters, design.lengthMeters);
      gl.uniform1f(gl.getUniformLocation(sceneProg, 'uTime'), timeSec);

      const numPts = Math.min(6, pointLights.length);
      gl.uniform1i(gl.getUniformLocation(sceneProg, 'uNumLights'), numPts);
      if (numPts > 0) {
        const flatPts = new Float32Array(24);
        for (let i = 0; i < numPts; i++) {
          flatPts[i * 4 + 0] = pointLights[i][0];
          flatPts[i * 4 + 1] = pointLights[i][1];
          flatPts[i * 4 + 2] = pointLights[i][2];
          flatPts[i * 4 + 3] = pointLights[i][3];
        }
        gl.uniform4fv(gl.getUniformLocation(sceneProg, 'uPointLights[0]'), flatPts);
      }

      gl.activeTexture(gl.TEXTURE1);
      gl.bindTexture(gl.TEXTURE_2D, shadowTex);
      gl.uniform1i(gl.getUniformLocation(sceneProg, 'uShadowMap'), 1);

      const uModel = gl.getUniformLocation(sceneProg, 'uModel');
      const uMap = gl.getUniformLocation(sceneProg, 'uMap');
      const uRough = gl.getUniformLocation(sceneProg, 'uRoughness');
      const uMetal = gl.getUniformLocation(sceneProg, 'uMetalness');
      const uBump = gl.getUniformLocation(sceneProg, 'uBump');
      const uEmissive = gl.getUniformLocation(sceneProg, 'uEmissive');
      const uOpacity = gl.getUniformLocation(sceneProg, 'uOpacity');
      const uSelected = gl.getUniformLocation(sceneProg, 'uSelected');
      const uIsFloor = gl.getUniformLocation(sceneProg, 'uIsFloor');

      for (let pass = 0; pass < 2; pass++) {
        if (pass === 1) {
          gl.enable(gl.BLEND);
          gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
          gl.depthMask(false);
        }
        for (const item of renderList) {
          const isTrans = item.opacity < 0.99;
          if ((pass === 0 && isTrans) || (pass === 1 && !isTrans)) continue;

          if (item.doubleSided) gl.disable(gl.CULL_FACE);
          else gl.enable(gl.CULL_FACE);

          gl.uniformMatrix4fv(uModel, false, item.modelMatrix);
          gl.activeTexture(gl.TEXTURE0);
          gl.bindTexture(gl.TEXTURE_2D, item.matEntry.texture);
          gl.uniform1i(uMap, 0);
          gl.uniform1f(uRough, item.matEntry.roughness);
          gl.uniform1f(uMetal, item.matEntry.metalness);
          gl.uniform1f(uBump, item.matEntry.bump || 0.0);
          gl.uniform1f(uEmissive, item.emissive || 0.0);
          gl.uniform1f(uOpacity, item.opacity);
          gl.uniform1f(uSelected, item.selected ? 1.0 : 0.0);
          gl.uniform1f(uIsFloor, item.isFloor ? 1.0 : 0.0);

          gl.bindVertexArray(item.gpuMesh.vao);
          gl.drawElements(gl.TRIANGLES, item.gpuMesh.count, gl.UNSIGNED_SHORT, 0);
        }
        if (pass === 1) {
          gl.depthMask(true);
          gl.disable(gl.BLEND);
        }
      }

      // Pass 3: SSAO + Bloom + ACES Filmic Composite
      gl.disable(gl.DEPTH_TEST);
      gl.disable(gl.CULL_FACE);
      gl.bindVertexArray(quadVao);

      if (realistic > 0.5) {
        // SSAO Pass
        gl.bindFramebuffer(gl.FRAMEBUFFER, ssaoA.fbo);
        gl.viewport(0, 0, ssaoA.w, ssaoA.h);
        gl.useProgram(ssaoProg);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, sceneTarget.depthTex);
        gl.uniform1i(gl.getUniformLocation(ssaoProg, 'uDepth'), 0);
        gl.uniformMatrix4fv(gl.getUniformLocation(ssaoProg, 'uInvProj'), false, invProjMatrix);
        gl.uniformMatrix4fv(gl.getUniformLocation(ssaoProg, 'uProj'), false, projMatrix);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

        // Blur SSAO horizontally & vertically
        gl.bindFramebuffer(gl.FRAMEBUFFER, ssaoB.fbo);
        gl.useProgram(blurProg);
        gl.bindTexture(gl.TEXTURE_2D, ssaoA.tex);
        gl.uniform1i(gl.getUniformLocation(blurProg, 'uTex'), 0);
        gl.uniform2f(gl.getUniformLocation(blurProg, 'uDir'), 1.1 / ssaoA.w, 0.0);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

        gl.bindFramebuffer(gl.FRAMEBUFFER, ssaoA.fbo);
        gl.bindTexture(gl.TEXTURE_2D, ssaoB.tex);
        gl.uniform2f(gl.getUniformLocation(blurProg, 'uDir'), 0.0, 1.1 / ssaoA.h);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

        // Bright pass for Bloom
        gl.bindFramebuffer(gl.FRAMEBUFFER, bloomA.fbo);
        gl.viewport(0, 0, bloomA.w, bloomA.h);
        gl.useProgram(brightProg);
        gl.bindTexture(gl.TEXTURE_2D, sceneTarget.tex);
        gl.uniform1i(gl.getUniformLocation(brightProg, 'uTex'), 0);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

        // Blur Bloom
        gl.bindFramebuffer(gl.FRAMEBUFFER, bloomB.fbo);
        gl.useProgram(blurProg);
        gl.bindTexture(gl.TEXTURE_2D, bloomA.tex);
        gl.uniform1i(gl.getUniformLocation(blurProg, 'uTex'), 0);
        gl.uniform2f(gl.getUniformLocation(blurProg, 'uDir'), 1.5 / bloomA.w, 0.0);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

        gl.bindFramebuffer(gl.FRAMEBUFFER, bloomA.fbo);
        gl.bindTexture(gl.TEXTURE_2D, bloomB.tex);
        gl.uniform2f(gl.getUniformLocation(blurProg, 'uDir'), 0.0, 1.5 / bloomA.h);
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
      }

      gl.bindFramebuffer(gl.FRAMEBUFFER, null);
      gl.viewport(0, 0, w, h);
      gl.useProgram(compProg);
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, sceneTarget.tex);
      gl.uniform1i(gl.getUniformLocation(compProg, 'uScene'), 0);
      gl.activeTexture(gl.TEXTURE1);
      gl.bindTexture(gl.TEXTURE_2D, bloomA.tex);
      gl.uniform1i(gl.getUniformLocation(compProg, 'uBloom'), 1);
      gl.activeTexture(gl.TEXTURE2);
      gl.bindTexture(gl.TEXTURE_2D, ssaoA.tex);
      gl.uniform1i(gl.getUniformLocation(compProg, 'uSsao'), 2);
      gl.uniform1f(gl.getUniformLocation(compProg, 'uRealistic'), realistic);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

      gl.bindVertexArray(null);
    }

    return {
      gl,
      createBufferMesh: (data) => createBufferMesh(gl, data),
      deleteBufferMesh: (m) => deleteBufferMesh(gl, m),
      render
    };
  }

  global.Mat4 = Mat4;
  global.RendererLib = { createRenderer };
})(window);
