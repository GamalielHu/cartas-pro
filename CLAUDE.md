# CLAUDE.md — CartasPro

App Android en Kotlin + Jetpack Compose (Material 3), paquete `com.gamalielhu.cartaspro`, minSdk 26 / target 35.
Genera cartas formales (Solicitud, Notificación, Presentación/Cotización, Libre) y las exporta a PDF y DOCX
con firma manuscrita. Textos de usuario en español.

## Estructura (`app/src/main/java/com/gamalielhu/cartaspro`)
- `model/Carta.kt`: `Carta` (todos los campos), enums, y la hoja abstracta `Bloque` (Parrafo/Firmas).
- `model/ComposicionCarta.kt`: Carta -> List<Bloque> (orden y estilos de la carta). PDF, vista previa y DOCX salen de aquí.
- `model/Plantillas.kt`: cláusulas rápidas, referencia y estructura formal por tipo.
- `pdf/RenderizadorCarta.kt`: maqueta en puntos con StaticLayout (justificado, paginación por líneas, firmas
  que no se parten) y dibuja sobre cualquier Canvas; `GeneradorPdf` usa `PdfDocument`; la vista previa usa Bitmap.
- `docx/GeneradorDocx.kt`: OpenXML a mano + ZipOutputStream (sin POI). Orden de elementos según esquema.
- `data/`: `PreferenciasCarta` (DataStore, autoguardado de toda la carta), `FirmaStore` (filesDir/firma1|2.png),
  `Exportador` (cache/compartir + FileProvider `${applicationId}.archivos`, guardar con SAF).
- `ui/`: `CartasViewModel`, `screens/` (Inicio, Editor con pestañas Datos/Vista previa), `components/FirmaPad`.

## Compilar
`.github/workflows/build-apk.yml` (ubuntu, JDK 17 + cache Gradle, AGP 8.9.1, Kotlin 2.0.21, Gradle 8.14.3):
`assembleDebug` firmado con `app/firma.jks` (clave `cartaspro`), artefacto + Release `v1.N` con `CartasPro.apk`.
Si falla, errores en la rama `registro`. No hay Android SDK en el contenedor: solo se compila en Actions.
