# DVR Fluo Análisis

[![FIJI](https://img.shields.io/badge/Programa-FIJI%2FImageJ-red.svg)](https://imagej.net/software/fiji/)
[![Licencia](https://img.shields.io/badge/Licencia-BSD--3--Clause-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://adoptium.net/)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.23697402.svg)](https://doi.org/10.5281/zenodo.23697402)

<div align="center">
<img src="src/main/resources/DVR-Fluo-Analisis-logo.png" width="300" alt="DVR Fluo Análisis">
</div>

<div align="justify">
  
- Herramienta desarrollada para FIJI/ImageJ que automatiza la cuantificación de la señal de fluorescencia en imágenes de microscopía: segmenta los núcleos marcados con DAPI y mide intensidad y superficie de marca en los canales AF488 y AF594.

<div align="center">
  
# Secciones
  
**[![Descarga](https://img.shields.io/badge/-Descarga-brown.svg)](#-descarga)** · **[![Instalación](https://img.shields.io/badge/-Instalación-brown.svg)](#-instalación)** · **[![Guías](https://img.shields.io/badge/-Guías-brown.svg)](#-guía-completa)** · **[![Compilar](https://img.shields.io/badge/-Compilar-brown.svg)](#-compilar-desde-el-código)** · **[![Cómo citar](https://img.shields.io/badge/-Cómo%20citar-brown.svg)](#-cómo-citar)** · **[![Licencia](https://img.shields.io/badge/-Licencia-brown.svg)](#-licencia)**

</div>

---

## ⚠️ Requiere FIJI / ImageJ actualizado.
- Descargalo en: [FIJI/ImageJ](https://imagej.net/software/fiji/downloads)

---

## 📥 Descarga

Descargá la última versión del `.jar` desde la sección **Releases** de este repositorio.

- ⬇️ **[Ir a Releases](https://github.com/ezegrondona/DVR-Fluo-Analisis/releases)**

---

## 🔧 Instalación

1. Descargar el `.jar`.
2. Copiarlo en la carpeta `plugins/` de tu instalación de FIJI.
3. Reiniciar FIJI.
4. El plugin aparecerá en el menú `Plugins`.

---

## 📋 Guía Completa

Las dos guías se abren en el navegador, sin descargar nada:

- 📘 **[Guía de Uso](https://github.com/ezegrondona/DVR-Fluo-Analisis/blob/main/docs/Gu%C3%ADa%20de%20Uso%20-%20DVR%20Fluo%20An%C3%A1lisis.pdf)**
- 📐 **[Guía de Calibración de FIJI/ImageJ](https://github.com/ezegrondona/DVR-Fluo-Analisis/blob/main/docs/Gu%C3%ADa%20de%20Calibraci%C3%B3n%20FIJI-ImajeJ.pdf)**

También desde los botones **GUÍA DE USO** y **GUÍA DE CALIBRACIÓN** del propio plugin.

---

## 👤 Autor

- **Autor:** Dr. Ezequiel Grondona
- **Institución:** Centro de Microscopía Electrónica — UNC / INICSA / CONICET
- **Contacto:** 📧 ezequiel.grondona@unc.edu.ar

---

## 🔨 Compilar desde el código

El complemento es un proyecto Maven. Con Java 8 o superior y Maven
instalados:

```
mvn clean package
```

El `.jar` listo para usar queda en `target/`. Incluye adentro las
bibliotecas que necesita —Apache POI, Log4j y sus dependencias—, así
que se copia a `plugins/` y funciona.

ImageJ no se empaqueta: la aporta la instalación de FIJI donde se
instale el complemento.

**Sobre el código fuente.** El original se perdió y se recuperó
descompilando el propio `.jar`; desde entonces se mantiene a mano, y
las correcciones y comentarios que tiene se escribieron sobre ese
código recuperado. Por eso algunos nombres de variable son los que
eligió la herramienta en lugar de los originales. El código publicado
acá es exactamente el que compila el `.jar` que se distribuye.

---

## 📑 Cómo citar

Si usás DVR Fluo Análisis en un trabajo publicado, citalo. Para una
herramienta científica, la cita es lo que permite que el trabajo de
construirla se reconozca y que otro pueda reproducir lo que hiciste.

> Grondona, E., & De Paul, A. L. (2026). *DVR Fluo Análisis* [software].
> Centro de Microscopía Electrónica, Universidad Nacional de Córdoba —
> INICSA (CONICET–UNC). https://doi.org/10.5281/zenodo.23697402

El repositorio incluye un archivo [`CITATION.cff`](CITATION.cff):
GitHub lo lee y muestra el botón **«Cite this repository»** arriba a
la derecha, que da la cita ya formateada en APA y en BibTeX.

**Sobre el DOI.** El de arriba es el *concept DOI*: apunta siempre a
la versión más reciente, así que una cita hecha hoy sigue sirviendo
cuando se publique una versión nueva. Si necesitás citar una versión
puntual —por ejemplo para que otro reproduzca exactamente tu análisis—
cada una tiene el suyo, listado en la solapa *Versions* del
[registro en Zenodo](https://doi.org/10.5281/zenodo.23697402).

---

## 📄 Licencia

**BSD 3-Clause.** Podés usar, modificar, redistribuir e incluso vender
obras derivadas, con dos condiciones: conservar el aviso de copyright
y la licencia, y no usar el nombre del autor ni el de la
institución para promocionar derivados sin permiso. El texto completo
está en [`LICENSE`](LICENSE).

El `.jar` que se distribuye empaqueta bibliotecas de terceros que
conservan sus propias licencias, todas permisivas y compatibles. Están
detalladas en [`THIRD-PARTY-NOTICES.txt`](THIRD-PARTY-NOTICES.txt).

</div>
