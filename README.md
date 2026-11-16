# CartasPro

Cartas formales (solicitudes, notificaciones, presentaciones/cotizaciones y cartas libres) listas en PDF o Word,
con tu firma dibujada con el dedo o cargada desde la galería.

**Descargar:** última versión en [Releases](../../releases/latest) → `CartasPro.apk`.

```
cartas-pro/
├── .github/workflows/build-apk.yml
├── build.gradle.kts · settings.gradle.kts · gradle.properties
├── gradle/libs.versions.toml · gradle/wrapper/
└── app/
    ├── build.gradle.kts · firma.jks
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/ (xml/rutas_archivos.xml, values/, drawable/, mipmap-anydpi-v26/)
        └── java/com/gamalielhu/cartaspro/
            ├── MainActivity.kt
            ├── model/   Carta.kt · ComposicionCarta.kt · Plantillas.kt
            ├── data/    PreferenciasCarta.kt · FirmaStore.kt · Exportador.kt
            ├── pdf/     RenderizadorCarta.kt · GeneradorPdf.kt
            ├── docx/    GeneradorDocx.kt
            └── ui/      CartasViewModel.kt · theme/Tema.kt · components/FirmaPad.kt
                         screens/AppCartas.kt · InicioScreen.kt · EditorScreen.kt
```
