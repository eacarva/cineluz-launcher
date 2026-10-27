# Abrir Configurações

App auxiliar (12 KB) que abre telas de configuração que o menu de algumas TV boxes
esconde: as Configurações completas do Android, "Sobre o dispositivo" e as
**Opções do desenvolvedor**. Serve para ligar a **depuração USB / ADB** em boxes
travadas, onde tocar 7 vezes no "Número da versão" não funciona pelo menu de fábrica.

Mostra só os botões das telas que existem no aparelho.

## Testado

| Aparelho | Resultado |
|---|---|
| **UniTV S1** (Rockchip RK322x, Android 7.1.2) | ✅ O botão *Opções do desenvolvedor (tela direta, Android antigo)* abriu as Opções do desenvolvedor, com o menu de ADB pela rede |
| Projetor HY320 (Android 11) | ✅ Abre as Opções do desenvolvedor das Configurações de TV |

## Como usar

1. Instale o APK pelo pendrive ou pelo navegador da box.
2. Abra **Abrir Configurações** (no Cineluz: Apps).
3. Tente os botões de *Opções do desenvolvedor*. Se nenhum abrir, use *Sobre o
   dispositivo* e toque 7 vezes em *Número da versão*.
4. Nas Opções do desenvolvedor, ligue a *Depuração USB* e, se existir, o ADB pela rede.

## Compilar

```bash
./build.sh            # gera build/abrir-configuracoes.apk
./build.sh install    # e instala no aparelho conectado ao adb
```
