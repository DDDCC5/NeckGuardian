# Sources and third-party components

- App: Kotlin, SpatialUI Compose and PICO Spatial SDK BOM 6.2.8. SDK/runtime dependencies are resolved from their official Maven repositories; the SDK itself is not redistributed as source. Their respective terms and licenses apply.
- AndroidX and Gradle build dependencies retain their respective licenses. The Gradle wrapper is included for reproducible builds.
- Models and UI artwork were authored for this project with procedural geometry, original SVG/vector designs and the approved app identity. Tools: Three.js (MIT), Rapier JavaScript compatibility package (Apache-2.0), and resvg (MPL-2.0) through @resvg/resvg-js. Tool versions are recorded in `production/tools/package-lock.json`.
- The optional asset preview vendors Three.js; its MIT license is retained in `production/preview/vendor/three/LICENSE`.
- Speech scripts are project-authored; audio was synthesized with Microsoft neural voices via edge-tts: zh-CN-YunyangNeural and en-US-GuyNeural. Runtime speech is bundled WAV audio. It is not a recording of the developer's own voice. Synthesis metadata is retained in `production/audio-catalog.json`.
- Button, warning, star-collection, success/end and block-topple effects were synthesized for this project; they are not third-party field recordings.

No permissive open-source license is granted for the project as a whole by this notice. Publishing this repository makes the source available for review; dependency licenses do not automatically license the application or third-party speech services.
