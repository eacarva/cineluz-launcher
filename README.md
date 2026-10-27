# Cineluz Launcher

**Tela inicial para projetores Android, com o HDMI em primeiro lugar.**

Leve (~50 KB), sem anúncios e sem acesso à internet. Feito para projetores
Android (testado no **HY320**), mas funciona em qualquer TV box ou projetor com
Android 7 ou mais novo.

*English: a lightweight, ad-free home screen for Android projectors that puts the
HDMI input first, with a live, muted preview of what's playing.*

## O que ele faz

- **HDMI ao vivo e mudo** — o bloco principal mostra o que está passando na
  entrada HDMI, sem som. OK abre em tela cheia; o controle remoto continua
  comandando o aparelho ligado no HDMI (HDMI-CEC).
- **Qualquer porta HDMI** — as portas são descobertas sozinhas. Se você ligar um
  aparelho em outra porta, a prévia passa para ela. Mostra até o nome do aparelho
  (via HDMI-CEC), como "HDMI 2 · Fire TV".
- **Abrir o HDMI ao ligar** — opcional, numa chave na própria tela inicial.
- **Blocos configuráveis** — AirPlay, Miracast, Arquivos e Configurações já vêm
  apontando para o app certo do seu aparelho, mas qualquer bloco pode abrir o app
  que você quiser.
- **Indicador de rede** — mostra Wi-Fi (nome e sinal) ou cabo; OK abre a lista de
  redes Wi-Fi.
- **Descanso de tela próprio** — relógio grande, sem anúncios (substitui os
  descansos de tela com propaganda que alguns fabricantes instalam).

## Como usar

| No controle | O que acontece |
|---|---|
| OK no bloco HDMI | Abre a entrada HDMI em tela cheia |
| Segurar OK (ou Menu) no bloco HDMI | Passa para a próxima porta HDMI |
| Segurar OK (ou Menu) em AirPlay, Miracast, Arquivos ou Configurações | Escolher o app daquele bloco |
| Home na tela inicial | Reinicia a prévia do HDMI (se a imagem sumir) |
| Apps → último item | Tela "Sobre o Cineluz" |

## Instalar

1. Ative no aparelho **Opções do desenvolvedor → Depuração USB** (e, se houver,
   depuração pela rede).
2. Baixe o APK mais recente em [Releases](../../releases).
3. No computador, com o [adb](https://developer.android.com/tools/releases/platform-tools):

```bash
adb connect IP-DO-PROJETOR:5555
adb install cineluz-launcher-1.0.0.apk
adb shell cmd package set-home-activity com.elisson.cineluz/.HomeActivity
adb shell pm grant com.elisson.cineluz android.permission.ACCESS_FINE_LOCATION
```

A última linha é opcional: o Android só deixa ler o **nome** da rede Wi-Fi com essa
permissão. Sem ela, o indicador mostra apenas "Wi-Fi · conectado".

Para usar o **relógio do Cineluz** como descanso de tela:

```bash
adb shell settings put secure screensaver_components com.elisson.cineluz/com.elisson.cineluz.ClockDream
```

### Voltar ao launcher original

```bash
adb shell cmd package set-home-activity PACOTE.DO.LAUNCHER.ORIGINAL/.Atividade
```

(ou simplesmente `adb uninstall com.elisson.cineluz`: o Android volta para o
launcher do fabricante).

## Aparelhos

| Aparelho | Situação |
|---|---|
| Projetor HY320 (Allwinner, Android 11) | Testado: HDMI ao vivo e mudo, abertura ao ligar, CEC |
| Outros Allwinner | Deve funcionar igual (mesmo app de HDMI e mesmos ajustes de som) |
| Outros aparelhos com entradas HDMI | HDMI abre numa tela cheia do próprio Cineluz |
| TV box sem entrada HDMI | Funciona como launcher simples (sem o bloco HDMI ao vivo) |

Testou em outro aparelho? Abra uma [issue](../../issues) contando como foi.

## Compilar

Sem Gradle: só o JDK (11+) e o Android SDK (`build-tools` 34+ e `platforms;android-30`).

```bash
./build.sh            # gera build/cineluz-launcher-<versão>.apk
./build.sh install    # gera e instala no aparelho conectado ao adb
```

Cada push no GitHub compila o APK automaticamente; cada tag `v*` publica uma versão
assinada em Releases.

## Privacidade

O Cineluz **não tem permissão de internet** — não envia nada para lugar nenhum.
As permissões que ele pede:

- *Ajustes de áudio* — deixar a prévia do HDMI sem som.
- *Estado da rede / Wi-Fi* — o indicador de rede.
- *Localização* — exigida pelo Android só para ler o nome da rede Wi-Fi. Opcional.

## Apoie

O Cineluz é gratuito e vai continuar assim. Se ele foi útil e você quiser agradecer
com um cafezinho, qualquer valor via **PIX** ajuda a manter o projeto:

<img src="docs/pix-qr.png" alt="QR code PIX" width="200">

- **Chave PIX (e-mail):** `contato@elissonandrade.com.br`
- **PIX copia e cola:**
  ```
  00020126510014br.gov.bcb.pix0129contato@elissonandrade.com.br5204000053039865802BR5915ELISSON ANDRADE6007MARINGA62110507CINELUZ63041688
  ```

Obrigado! 🙏

## Licença

[Apache License 2.0](LICENSE). Ícones do
[Material Symbols](https://github.com/google/material-design-icons) (Google, Apache 2.0).
