# Verification: 1.7 (versionCode 8)

- Local `assembleDebug testDebugUnitTest` passed:22JVM tests,0failures. Existing scoring/angle/history rules are unchanged.
- Experimental API, design style and placement source checks passed; source accessibility audit has0errors and0warnings.
- Textured GLB variants pass glTF Validator with0errors/0warnings; original geometry and animation buffers are preserved.
- Release APK retains38WAVs and the approved launcher resources; v2signature verification passes.
- The APK was replace-installed on the connected real PICO headset on2026-11-24, then launched; process remained alive and a15-second watcher found no crash.
- Web preview interactions were reviewed; tower/result consistency checks reported0mismatches. Example values and Web rendering are not headset evidence.
- Current local emulator lacks ROM spatial capture/A11Y extensions and displays a firmware compatibility gate forSDK6.2.8; new runtime accessibility, headset visual/gesture acceptance, tracking accuracy and prolonged performance remain pending. The previous tester-reported crash has no supplied exception stack; this version does not claim that issue fixed.

## Earlier 1.6 verification

# Verification: 1.6 (versionCode 7)

- `assembleDebug testDebugUnitTest`: passed, 22 JVM tests, 0 failures.
- Tests cover tower grace/recovery/timing, weighted averages, sustained steering, angle math, range maxima, history limit, unique swept star contact, half-size collision geometry, missed/collected exclusivity and completion, and authored tower playback interpolation.
- APK signature verification passed. Runtime package contains 9 GLBs, 38 WAVs and the approved launcher icon. Newly introduced self-report icons are native vectors, with distinct shapes and colors.
- 1.6 was installed over the earlier headset build. The creator subsequently reported no remaining significant issue and requested publication. Earlier real-device checks received official HMD pose and exercised training/window transitions; emulator checks validated menus, examples of actual flight counts, results/history and all three self-report bands.
- UI style, experimental-API and source accessibility audits passed. Scoped runtime accessibility checks passed for the native preview and Stage controls. Uncaptured 3D components were not treated as fully audited.

Physical head-angle accuracy, prolonged performance and universal gesture behavior across devices have not been independently certified. This is a debug-signed experience build, not a medical device. Screenshots in the submission folder identify their capture environment; artwork is not presented as runtime evidence.
