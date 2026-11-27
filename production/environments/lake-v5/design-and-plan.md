# Lakeside 3D pilot v5

User said继续after thin-foliage v4 review. Continue the already selected lake-first pilot. Preserve original app/training assets, real pose and scoring. No new paid Tripo tasks. Existing native Editor connection is unavailable; continue local asset authoring and browser scene review, label provenance accurately.

Use the approved v4 tree (original Tripo branch geometry plus locally rebuilt foliage), near shoreline, timber deck, quiet water, low distant hills and a few stones. Structural terrain/water/deck/bench/railings are locally authored; do not call them Tripo output. All foreground objects are true geometry. A seated eye at(0,1.25,1.8)m faces toward negativeZ; deck top isY0. Keep central training corridor x[-.8,.8], y[.5,2.2], z[-3.5,2.2] clear of scenery. Compute tree scale from actual GLB bounds and root-to-ground offset; share model resources across instances.

Implementation: standalone browser composition with meters-based transforms, actual existing plane/star GLBs, environment sound from approved lake.wav, seated/orbit/overview controls. Separate environment root from preview training objects. Export static environment GLB and current-camera PNG through visible browser controls. Preserve clear distinction between browser rendering and actual PICO runtime.

Verification: inspect seated/front, overview, sides and rear; verify actual object bounds/corridor, no floating ground contacts, no central rail, no model orientation regression. Audit exported GLB for embedded resources and glTF errors. Export transient animations as static meshes and list differences (lighting/fog/water motion are browser-preview behavior, not yet native implementation). No APK replacement before full-scene visual approval.
