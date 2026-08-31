# Ritmo - Pedômetro Android

Projeto Kotlin para Android Studio que implementa a proposta 4 do enunciado: rastreador de passos e ritmo de corrida.

## Recursos

- Leitura de `TYPE_STEP_COUNTER`, com `TYPE_STEP_DETECTOR` e acelerômetro como alternativas.
- Permissão de atividade física em Android 10 ou superior.
- Cadência em passos por minuto (SPM), distância e calorias estimadas pela passada e peso do perfil.
- Histórico diário persistido com Room e gráfico de evolução dos últimos sete dias.
- Interface nativa construída com Jetpack Compose.

## Como abrir

Abra esta pasta no Android Studio, aguarde a sincronização do Gradle e execute em um aparelho físico. Emuladores normalmente não fornecem dados reais de sensores; quando não houver contador de passos, o app usa a leitura do acelerômetro como alternativa.
