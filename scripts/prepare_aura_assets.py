"""Build local, deterministic brand assets. Run manually; never from Gradle."""
from pathlib import Path
from urllib.request import urlopen
from io import BytesIO
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "designsystem/src/commonMain/composeResources"


def main():
    font_dir = RES / "font"
    font_dir.mkdir(parents=True, exist_ok=True)
    license_dir = RES / "files"
    license_dir.mkdir(parents=True, exist_ok=True)
    base = "https://raw.githubusercontent.com/google/fonts/main/ofl/manrope/"
    with urlopen(base + "Manrope%5Bwght%5D.ttf") as response:
        data = response.read()
    for name, weight in [("regular", 400), ("medium", 500), ("semibold", 600), ("bold", 700)]:
        font = instantiateVariableFont(TTFont(BytesIO(data)), {"wght": weight}, inplace=False)
        font.save(font_dir / f"manrope_{name}.ttf")
    with urlopen(base + "OFL.txt") as response:
        (license_dir / "manrope_OFL.txt").write_bytes(response.read())
    print("Prepared Manrope 400/500/600/700 with OFL license.")


if __name__ == "__main__":
    main()
