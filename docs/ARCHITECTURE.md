# Architecture

`com.example.neckguardian` is a Kotlin/SpatialUI Compose PICO Spatial application.

- Native Shared Space window: consent dialog, home, six-question self-report, activity selection/settings, results and history.
- Mixed passthrough Full Space Stage: tower and flight challenges, extension and side-bend checks. Official HMDTrackingProvider provides fresh real head pose and neutral calibration. Shared Space cannot provide those angles; training backgrounds other applications. There is no fake-angle fallback or cross-app background posture monitor.
- Native setup previews use separate decorative timelines. They never change real pose, session time or score.
- Tower: configurable angle/duration/grace, time-weighted mean, continuous grace/reset and authored whole-block topple animation. Original GLB animation samples are interpolated without regenerating geometry.
- Flight: sustained side tilt steers a half-size paper plane. Rendering and swept star contact share a coordinate path and plane scale. Collected and missed IDs are mutually exclusive; passed stars count once. Completion = collected / (collected + missed), excluding unresolved in-flight stars when ending a session.
- Angle checks: front-facing extension bar and side-bend dial; independently record extension/left/right maxima.
- History: latest 30 private local records. No app account or cloud sync. System language selects Simplified Chinese or English; speech follows the same selection and system volume.

Self-report grouping is a simple, non-clinically validated heuristic. The application provides wellbeing reference only and makes no medical diagnosis or treatment claim.
