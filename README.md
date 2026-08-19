# StagePulse System Design

Tablet-first live-event system design and rider tool for StagePulse.

## Current product capabilities

- Filament-based 3D GLB/GLTF scene rendering.
- Stage dimensions entered in metres.
- Equipment library for PA, monitors, instruments, microphones, IEM, rigging, lighting, video, control and power.
- GLB assets bundled under `apps/src/main/assets/models/`.
- Touch selection and MOVE mode.
- XYZ positioning and RX/RY/RZ rotation.
- Camera orbit and two-finger zoom.
- Undo/redo project snapshots.
- Project JSON save/open.
- Stage plot / rider PDF export.
- SPL estimate and stage-area heatmap reporting.
- Unit tests for the acoustic calculation layer.
- GitHub Actions validation, debug APK, release APK and release AAB generation.

## Acoustic safety

The acoustic engine is a design-estimate model. Sensitivity, power, maximum SPL and coverage values are reference metadata unless a specific manufacturer/cabinet dataset is entered. Exact deployment must be checked against the selected manufacturer's datasheet, controller presets, DSP limits, environmental conditions and field measurements.

## Build

GitHub Actions uses JDK 17, Android API 36 and Gradle 9.5.0. The workflow validates the project, validates bundled GLB files, runs tests and produces debug/release APK plus release AAB artifacts.
