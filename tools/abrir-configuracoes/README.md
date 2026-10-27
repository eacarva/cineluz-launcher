# Abrir Configurações

App auxiliar (20 KB) que abre telas de configuração que o menu de algumas TV boxes
esconde: as Configurações completas do Android, "Sobre o dispositivo" e as
**Opções do desenvolvedor**. Serve para ligar a **depuração USB / ADB** em boxes
travadas, onde tocar 7 vezes no "Número da versão" não funciona pelo menu de fábrica.

Também abre a tela do **Google Play Services** e da **Play Store**, com o botão
*Desativar/Ativar*: em boxes com pouca memória que não usam a Play Store, desativar o
Play Services libera cerca de 180 MB (reative pelo mesmo botão quando precisar). Se o
*Desativar* estiver bloqueado, primeiro desmarque o Play Services em *Administradores do
dispositivo* (botão também incluído).

Mostra só os botões das telas que existem no aparelho.

## Testado

| Aparelho | Resultado |
|---|---|
| **UniTV S1** (Rockchip RK322x, Android 7.1.2) | ✅ Abre as Opções do desenvolvedor (com o menu de ADB pela rede) e desativa/reativa o Google Play Services |
| Projetor HY320 (Android 11) | ✅ Abre as Opções do desenvolvedor das Configurações de TV |

## Como usar

1. Instale o APK pelo pendrive ou pelo navegador da box.
2. Abra **Abrir Configurações** (no Cineluz: Apps). A tela mostra passos numerados,
   cada um com uma explicação embaixo:
   - **1. Ligar o ADB:** *Abrir as Opções do desenvolvedor* tenta sozinho todos os
     caminhos conhecidos. Se não abrir, *Ative o modo desenvolvedor* leva a "Sobre o
     dispositivo" para os 7 toques em "Número da versão".
   - **2. Serviços do Google:** mostra se o Play Services e a Play Store estão ativos e
     guia o que fazer (inclusive liberar o *Desativar* quando o Google é administrador
     do dispositivo). O mesmo botão reativa.
   - **3. Outros:** Configurações completas do Android.

## Compilar

```bash
./build.sh            # gera build/abrir-configuracoes.apk
./build.sh install    # e instala no aparelho conectado ao adb
```
