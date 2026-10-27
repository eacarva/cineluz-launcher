<p align="center">
  <img src="docs/icon.svg" alt="Ícone do Cineluz Launcher: uma lente que projeta um feixe de luz âmbar numa tela" width="120">
</p>

# Cineluz Launcher — launcher para projetor Android com HDMI em primeiro lugar

**Cineluz Launcher** é uma tela inicial (launcher) gratuita e de código aberto para
**projetores Android** e TV boxes, feita para quem usa o projetor principalmente pela
**entrada HDMI**. Ele mostra uma **prévia ao vivo e sem som do HDMI** na tela inicial,
pode **abrir direto no HDMI ou no seu app principal ao ligar**, não tem **nenhum anúncio** e não acessa a
internet. Foi criado e testado no **projetor HY320 (Magcubic, Allwinner H713,
Android 11)** e funciona em outros projetores e TV boxes com Android 7 ou mais novo.

![Tela inicial do Cineluz Launcher no projetor HY320, com o bloco HDMI em destaque](docs/screenshot-home.png)

> 🇬🇧 **English:** Cineluz Launcher is a free, open-source, ad-free **home screen for
> Android projectors** (tested on the **HY320 / Magcubic, Allwinner H713**) that puts the
> **HDMI input first**: live muted HDMI preview on the home screen, auto-open HDMI on
> boot, any HDMI port detected automatically. [Read in English ↓](#english)

## Por que usar

Muitos projetores Android baratos (HY300, HY320, L018 e parecidos) vêm com um launcher
de fábrica **cheio de anúncios**, com o HDMI escondido no meio de atalhos que você não
usa, e às vezes com um **descanso de tela que mostra propaganda em vídeo**. Launchers
alternativos de Android TV costumam **não mostrar a entrada HDMI** ou travar ao trocar
para ela. O Cineluz resolve exatamente isso:

- **Bloco HDMI ao vivo e mudo** — veja na tela inicial o que está passando no aparelho
  ligado ao HDMI (TV box, videogame, Fire TV, Chromecast…), sem som. OK abre em tela cheia.
- **Abrir o HDMI ou um app ao ligar** — liga o projetor e ele vai direto para a entrada
  HDMI, ou para o app que você escolher (YouTube, IPTV, Netflix…). Opcional, numa chave
  na própria tela inicial.
- **TV box sem HDMI** — o bloco grande vira o seu **app principal**, que também pode
  abrir sozinho ao ligar.
- **Qualquer porta HDMI** — as portas são detectadas sozinhas; ligue um aparelho em
  outra porta e a prévia muda para ela. Mostra até o nome do aparelho via **HDMI-CEC**.
- **O controle remoto do projetor continua comandando o aparelho no HDMI** (HDMI-CEC).
- **Sem anúncios, sem internet** — o app nem tem permissão de acesso à internet.
- **Relógio por inatividade** — aparece antes do descanso de tela do fabricante (que em
  alguns projetores mostra propaganda em vídeo) e muda de lugar para não marcar a imagem.
- **Blocos configuráveis** — AirPlay (AirPin), Miracast, Arquivos e Configurações já
  vêm apontando para os apps certos do aparelho; qualquer bloco pode abrir outro app.
- **Indicador de Wi-Fi** — mostra a rede e o sinal; um clique abre a lista de redes.
- **Levíssimo** — cerca de 50 KB (o launcher de fábrica do HY320 tem ~21 MB).

## Compatibilidade

| Aparelho | Situação |
|---|---|
| **Projetor HY320** (Magcubic, Allwinner H713, Android 11) | ✅ Testado: HDMI ao vivo e mudo, abrir HDMI ao ligar, HDMI-CEC |
| **HY300, L018 e outros projetores Allwinner** | Deve funcionar igual (mesmo app de HDMI e mesmos ajustes de som) — [conte como foi](../../issues) |
| Outros projetores / TVs Android com entrada HDMI | HDMI abre na tela cheia do próprio Cineluz |
| TV box sem entrada HDMI (Mi Box, Fire TV, genéricas) | Funciona: o bloco grande vira o app principal, que pode abrir ao ligar |

Requer **Android 7.0 ou mais novo**. Não precisa de root.

## Como instalar

1. **Baixe o APK** — no próprio projetor (pelo navegador ou pelo app *Downloader*) ou
   no celular/computador para passar por pendrive:

   **[⬇️ Baixar o Cineluz Launcher](https://github.com/eacarva/cineluz-launcher/releases/latest/download/cineluz-launcher.apk)**

2. **Instale** — abra o arquivo baixado. Se o Android pedir, permita instalar apps
   desse app (*fontes desconhecidas*).
3. **Abra o Cineluz** e confirme quando ele perguntar se deve ser a **tela inicial**.

Pronto. Ele também pede, uma vez, a permissão de localização — o Android só deixa um
app ler o **nome da rede Wi-Fi** com ela. Pode recusar sem problema.

### Desinstalar

*Configurações → Apps → Cineluz Launcher → Desinstalar.* O projetor volta sozinho
para a tela inicial de fábrica.

## Como usar

| No controle | O que acontece |
|---|---|
| **OK** no bloco HDMI | Abre a entrada HDMI em tela cheia |
| **Segurar OK** (ou **Menu**) no bloco HDMI | Passa para a próxima porta HDMI |
| **Segurar OK** (ou **Menu**) em AirPlay, Miracast, Arquivos ou Configurações | Escolher o app daquele bloco |
| **Home** na tela inicial | Reinicia a prévia do HDMI (se a imagem sumir) |
| Botão **Ao ligar ⌄** (ex.: *HDMI ⌄*) | Escolher o que abrir ao ligar: HDMI ou um app |
| Chave verde/vermelha ao lado | Liga/desliga a abertura automática ao ligar o aparelho |
| **OK** / **Segurar OK** no bloco grande (aparelho sem HDMI) | Abre o app principal / troca o app |
| Símbolo do Cineluz no canto inferior direito | Sobre o Cineluz e como apoiar o projeto |

Se ninguém mexer no controle por alguns minutos na tela inicial, aparece um **relógio**
que muda de lugar a cada minuto (para não marcar a imagem). Qualquer tecla volta.

## Perguntas frequentes

**Como tirar os anúncios do projetor HY320 (ou HY300)?**
Os anúncios vêm do launcher de fábrica e do descanso de tela dele. Instale o Cineluz e
escolha-o como tela inicial: a tela de fábrica sai de cena e, quando o projetor fica
parado, aparece o relógio do Cineluz no lugar do descanso de tela com propaganda.
Não precisa trocar o firmware, fazer root nem usar computador.

**Como fazer o projetor Android abrir direto no HDMI ao ligar?**
Com o Cineluz como tela inicial, deixe *Ao ligar* em *HDMI* e a chave verde. Na
primeira vez que a tela inicial aparece depois de ligar, ele abre a entrada HDMI
que tem sinal.

**Como fazer a TV box ou o projetor abrir um app sozinho ao ligar?**
Na barra *Ao ligar*, aperte OK no botão com a setinha (*HDMI ⌄*) e escolha o app (por
exemplo YouTube ou o app de IPTV). Para trocar depois, é só repetir. Com a chave verde, o
app abre sozinho depois de ligar.

**Por que outros launchers (Projectivy, FLauncher) não mostram ou travam o HDMI?**
No HY320 e em outros projetores Allwinner, o HDMI é aberto por um app do fabricante
que precisa receber qual entrada abrir. Launchers genéricos abrem esse app sem essa
informação (ou nem o enxergam, porque ele não aparece como app comum). O Cineluz abre
do mesmo jeito que o launcher de fábrica e, em outros aparelhos, usa a entrada HDMI
direto pelo sistema de TV do Android.

**O controle do projetor continua funcionando no aparelho ligado ao HDMI?**
Sim. O repasse das teclas (HDMI-CEC) é feito pelo sistema do projetor, e o Cineluz
abre o HDMI da mesma forma que o launcher original.

**Por que a prévia do HDMI fica sem som?**
De propósito: a tela inicial fica silenciosa. Ao abrir o HDMI, o som volta normalmente.

**Funciona em TV box, Fire TV ou Android TV sem entrada HDMI?**
Sim. Sem entrada HDMI, o bloco grande vira o **app principal**: na primeira vez ele
pede para escolher o app, e depois OK abre esse app. Ele também pode abrir sozinho ao
ligar a box.

**Ele coleta dados ou precisa de internet?**
Não. O Cineluz não tem permissão de internet. As permissões usadas são: ajustes de
áudio (deixar a prévia muda), estado da rede/Wi-Fi (indicador de rede) e,
opcionalmente, localização (o Android só deixa ler o nome do Wi-Fi com ela).

**Preciso de computador, root ou firmware modificado?**
Não. É um app comum: baixe, instale e escolha como tela inicial. Dá para desinstalar a
qualquer momento.

## Para desenvolvedores

Compila sem Gradle — só o JDK 11+ e o Android SDK (`build-tools` 34+ e
`platforms;android-30`):

```bash
./build.sh            # gera build/cineluz-launcher-<versão>.apk
./build.sh install    # gera e instala no aparelho conectado ao adb
```

Cada push compila o APK no GitHub Actions; cada tag `v*` publica uma versão assinada em
Releases. Em aparelhos que deixam escolher o descanso de tela, o Cineluz também oferece
o *Relógio do Cineluz* como descanso de tela do sistema. Relatos de outros aparelhos e
contribuições são bem-vindos em [Issues](../../issues).

## Apoie

O Cineluz é gratuito e vai continuar assim. Se ele foi útil e você quiser agradecer
com um cafezinho, qualquer valor via **PIX** ajuda a manter o projeto:

<img src="docs/pix-qr.png" alt="QR code PIX para apoiar o Cineluz Launcher" width="200">

- **Chave PIX (e-mail):** `contato@elissonandrade.com.br`
- **PIX copia e cola:**
  ```
  00020126510014br.gov.bcb.pix0129contato@elissonandrade.com.br5204000053039865802BR5915ELISSON ANDRADE6007MARINGA62110507CINELUZ63041688
  ```

Obrigado! 🙏

---

## English

**Cineluz Launcher** is a free, open-source, lightweight (~50 KB) **Android launcher
for projectors** and TV boxes, built for people who mainly use their projector through
the **HDMI input**.

- **Live, muted HDMI preview** on the home screen; press OK to open HDMI full screen.
- **Auto-open HDMI or any app on boot** (the *Ao ligar ⌄* button picks what to open; the
  toggle next to it turns it on/off). On TV boxes without HDMI, the big tile becomes your **main app**.
- **Any HDMI port** is detected automatically through Android's TV input framework;
  the preview follows the port where a device was just plugged in, and shows the device
  name via **HDMI-CEC**. The projector remote keeps controlling the HDMI device.
- **No ads, no internet permission.** An idle **clock** (moving every minute to avoid
  burn-in) shows up before manufacturer screensavers that play video ads.
- Configurable tiles (AirPlay, Miracast, Files, Settings), Wi-Fi indicator, app drawer.

**Tested on the HY320 projector (Magcubic, Allwinner H713, Android 11).** Other
Allwinner projectors (HY300, L018…) should behave the same; other devices with HDMI
inputs open HDMI in Cineluz's own full-screen player. Requires Android 7.0+, no root.

**Install:** [download the APK](https://github.com/eacarva/cineluz-launcher/releases/latest/download/cineluz-launcher.apk),
open it on the projector (allow unknown sources if asked), open Cineluz and confirm it
as the home app. To go back, uninstall it from Settings → Apps.

The app UI is currently in Brazilian Portuguese; translations are welcome.

## Licença / License

[Apache License 2.0](LICENSE). Ícones / icons:
[Material Symbols](https://github.com/google/material-design-icons) (Google, Apache 2.0).
