# StagePulse System Design

Tablet-first StagePulse system-design and rider workspace.

## Included

- Filament-based real-time 3D renderer.
- GLB/glTF equipment asset library under `apps/src/main/assets/models`.
- SVG stage-plot symbols under `apps/src/main/assets/svg`.
- Equipment placement, move mode, camera orbit/zoom, XYZ and rotation controls.
- Editable stage width/depth/height.
- Equipment technical metadata and acoustic reference parameters.
- SPL / distance / directivity calculation engine with unit tests.
- Project JSON export.
- Rider PDF export with stage plot and equipment coordinates.
- GitHub Actions validation, unit tests, debug APK build, APK existence/size check and artifact upload.

## Build

GitHub Actions installs JDK 17, Android SDK 36 and Gradle 9.7, runs tests, builds the debug APK and refuses to publish an artifact if no APK is produced.

## Important acoustic note

The acoustic engine is a deterministic planning model, not a substitute for manufacturer prediction software or an on-site measurement. Cabinet sensitivity, maximum SPL, power and coverage values must be checked against the exact deployed product datasheet before a professional deployment.

The included 3D assets are StagePulse-created generic reference models and are not manufacturer CAD models. They are intended to provide a consistent visual system-design representation without copying proprietary manufacturer geometry.
