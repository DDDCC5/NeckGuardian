# Native integration · lake v5

User confirmed continuing from the whole-scene preview on 2026-11-27. Scope: replace only the LAKE flight environment with the accepted exported GLB, keep other environment choices, original training GLBs, IMU, voice, collisions and scoring.

The environment is an independent child of the existing identity placementOwner. Load at scale1 meters, without collider or gesture surface. Map authored seated eye (0,1.25,1.8) to fresh official HMD position using yaw-only rotation. Freeze this world transform until an explicit recenter revision; never apply the old panorama translation-follow update to geometry. Plane/controls retain their existing separate scene root and HMD-relative initial placement. No new SpatialView sizing or container architecture.

Lighting uses retained SDR SRGB IBL with existing exponent8; review native material brightness and add one documented warm directional light if needed. No new experimental API. Teardown destroys the parent then closes owned IBL resources; cancellation destroys partial scene and closes retained resources. REAL returns no scenery; rejected LAKE panorama is lighting-only and never drawn.

Validation: route regression unit test, assembleDebug+all domain tests, API/style/placement/project A11Y checks, replace-install real PICO real device, open static flight prestart and scoped runtime audit, collect model bounds/real pose/ambient logs and valid spatial PNG, verify return restores window/stops ambience. User deferred wearing the HMD this turn: seated comfort, translation parallax, leaf quality and performance acceptance remain pending. Do not start gameplay or manufacture acceptance from a resting headset.
