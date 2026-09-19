# Ghirlande

Mod Fabric per **Minecraft Java 26.3**.

## Crafting

Una ghirlanda si crea in una crafting table 3x3 usando otto fiori supportati e lasciando vuoto il centro:

```text
F F F
F   F
F F F
```

Il colore della ghirlanda deriva dal pigmento vanilla prodotto dai fiori. Fiori diversi che producono lo stesso pigmento possono essere mescolati. Se nella ricetta vengono mescolati colori diversi, la ghirlanda rimane cosmetica e non concede bonus.

La ghirlanda può essere equipaggiata esclusivamente nello slot della testa e non fornisce punti armatura.

## Effetti

| Colore | Effetto |
|---|---|
| Rosso | +2 cuori massimi |
| Giallo | Velocità I |
| Blu | Salto potenziato I |
| Bianco | Knockback I quando la mano principale è vuota |
| Arancione | Resistenza al fuoco |
| Rosa | Addomestica, avvia l'accoppiamento o cura di 1 cuore con click destro a mano vuota; 10 utilizzi |
| Magenta | Haste I |
| Azzurro | Respiration I |
| Grigio chiaro | Resistance I |
| Ciano | Visione notturna |
| Nero | Strength I |
| Grigio | Shift + click destro a mano vuota replica la farina d'ossa; 20 utilizzi |
| Mista | Nessun effetto |

Le ghirlande rosa e grigia, una volta esaurite le cariche, **non vengono distrutte**: restano equipaggiabili e visibili ma il loro potere attivo smette di funzionare.

## Rendering

L'oggetto conserva gli otto fiori usati nel crafting. Il renderer client costruisce dinamicamente la ghirlanda con gli stessi fiori, evitando una texture statica per ogni combinazione possibile.

## Sicurezza del salvataggio

La mod non aggiunge blocchi, biomi, strutture o world generation. Le ghirlande memorizzano i propri dati soltanto nei rispettivi ItemStack. È comunque consigliato provare ogni nuova build in un mondo di test e conservare un backup prima di installarla nel mondo principale.

## Build

Target: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25, Gradle 9.6.

GitHub Actions compila il progetto ed esegue smoke test server/client prima di pubblicare l'artefatto di build.
