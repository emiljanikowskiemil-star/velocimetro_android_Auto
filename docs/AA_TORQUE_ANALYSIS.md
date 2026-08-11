# Análise Arquitetural do AA Torque (`aa-torque`)

Este documento detalha o funcionamento interno do projeto open-source **AA Torque**, com foco em como ele realiza a integração gráfica personalizada com o Android Auto sem se limitar aos templates pré-definidos da biblioteca oficial *Android for Cars App Library*.

---

## 1. Mecanismo de Renderização Gráfica no Android Auto

### Como o AA Torque renderiza interfaces/dashboards personalizados no carro?

Diferente das aplicações tradicionais de Android Auto baseadas em `CarAppService` e templates (*PaneTemplate*, *GridTemplate*, *NavigationTemplate*), o AA Torque utiliza o **Android Auto Projection SDK** (legado, fornecido através da biblioteca `aauto.aar` / `com.google.android.apps.auto.sdk` / `com.google.android.gms.car`).

Esse SDK habilita a criação de uma **`CarActivity`**, que roda como um contexto de janela/display virtual nativo do Android. Com isso:
1. É possível inflar layouts XML convencionais do Android (`setContentView(...)`).
2. Podem ser utilizados **Fragments** do Android (`supportFragmentManager`).
3. É possível desenhar em um `Canvas` customizado (`View.onDraw()`) e utilizar bibliotecas de animação e mostradores gráficos sem nenhuma limitação gráfica imposta pelos templates estruturados do Android Auto.

---

## 2. Registro e Publicação no Launcher do Android Auto

### `AndroidManifest.xml`

Para registrar a aplicação no Android Auto e torná-la visível no launcher do veículo, o AA Torque declara um serviço dedicado (`CarService`):

```xml
<service
    android:name="com.aatorque.stats.CarService"
    android:exported="true"
    android:label="@string/app_car_service_name"
    tools:ignore="ExportedService">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="com.google.android.gms.car.category.CATEGORY_PROJECTION" />
        <category android:name="com.google.android.gms.car.category.CATEGORY_PROJECTION_OEM" />
    </intent-filter>
</service>

<meta-data
    android:name="com.google.android.gms.car.application"
    android:resource="@xml/automotive_app_desc" />
```

### Recurso XML (`res/xml/automotive_app_desc.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<automotiveApp xmlns:tools="http://schemas.android.com/tools">
    <uses name="service" tools:ignore="InvalidUsesTagAttribute" />
    <uses name="projection" tools:ignore="InvalidUsesTagAttribute" />
</automotiveApp>
```

- A tag `<uses name="projection" />` informa ao Android Auto que a aplicação projeta sua própria interface diretamente no display da multimídia.
- As categorias `CATEGORY_PROJECTION` e `CATEGORY_PROJECTION_OEM` com a ação `MAIN` no `intent-filter` indicam ao launcher do Android Auto que este serviço inicia uma sessão de projeção customizada.

---

## 3. Fluxo de Inicialização e Renderização

```
[ Central Multimídia / Android Auto ]
                 │
                 ▼ (Lança app selecionado no Launcher)
    [ CarService (CarActivityService) ]
                 │
                 ▼ getCarActivity()
    [ MainCarActivity (CarActivity) ]
                 │
                 ▼ setContentView(R.layout.activity_car_main)
   [ Fragments / SpeedometerView (Canvas) ]
                 │
                 ▼ Atualizações em Tempo Real (5-10 Hz)
       [ StateFlow / Repository ]
```

1. **`CarService`**: Herda de `com.google.android.apps.auto.sdk.CarActivityService`. Implementa o método `getCarActivity()`, retornando a classe `MainCarActivity::class.java`.
2. **`MainCarActivity`**: Herda de `com.google.android.apps.auto.sdk.CarActivity` (que estende as funcionalidades de uma `FragmentActivity` normal do Android). Ela inicializa a janela no display do veículo, configura o tema, gerencia ciclo de vida (`onCreate`, `onStart`, `onResume`, `onSaveInstanceState`) e infla a hierarquia de Views ou Fragments.
3. **`DashboardFragment` / Custom Views**: Gerencia os gauges (`TorqueGauge`, `TorqueSpeedometer`, `SpeedView`). Utiliza o ciclo de desenho nativo do Android (`onDraw`) atualizado reativamente ou via ticks de dados.

---

## 4. Análise dos Componentes do AA Torque

| Componente | Classe / Arquivo | Função no AA Torque | Reaproveitamento para o Velocímetro GPS |
| :--- | :--- | :--- | :--- |
| **Service (AA)** | `CarService.java` | Ponto de entrada do Android Auto Projection SDK (`CarActivityService`) | **Manter e Adaptar**: Criar `AutoService` derivado de `CarActivityService`. |
| **Activity (AA)** | `MainCarActivity.kt` | Container principal exibido na multimídia do carro (`CarActivity`) | **Manter e Adaptar**: Criar `AutoSpeedometerActivity` herdando de `CarActivity`. |
| **Activity (App)** | `SettingsActivity.kt` | Configurações no smartphone | **Criar Nova**: `MainActivity` / `GpsSpeedometerActivity` com card grande e configurações. |
| **Biblioteca SDK** | `lib/aauto.aar` | Fornece as classes `CarActivity`, `CarActivityService`, `StatusBarController` | **Reaproveitar**: Incluir biblioteca `aauto.aar` no projeto. |
| **Comunicação** | DataStore / Broadcast | Sincroniza preferências e eventos de teclado/controle rotativo | **Adaptar**: Utilizar `StateFlow` / `Repository` singleton para compartilhar dados entre smartphone e AA. |
| **Conexão OBD2/Torque** | `TorqueService.kt`, `TorqueServiceWrapper.kt` | Integração via AIDL com o app Torque Pro | **REMOVER**: Não será utilizado; a fonte será 100% o GPS nativo (`FusedLocationProviderClient` / `LocationManager`). |

---

## 5. Licença GPL-3.0 e Atribuição

O AA Torque é disponibilizado sob a licença **GNU General Public License v3.0 (GPL-3.0)**.
Conforme as obrigações da licença GPL-3.0:
- Qualquer arquivo ou trecho de código derivado do AA Torque deve incluir a atribuição de autoria original (`agronick/aa-torque`) e o aviso de licença mantido.
- O projeto final derivado que inclua ou adapte este código também será sob GPL-3.0.

---

## 6. Possíveis Incompatibilidades e Mitigações

1. **Restrições em Versões Recentes do Android Auto**:
   - Em smartphones sem ROOT ou sem o *Android Auto Developer Mode* / *Unkown Sources* ativado, o Android Auto pode bloquear aplicativos terceiros que utilizem a projeção direta por `aauto.aar`.
   - **Solução/Procedimento**: Para testes no DHU (Desktop Head Unit) ou em centrais multimídia reais, é necessário ativar o **Modo de Desenvolvedor no Android Auto** do smartphone (7 toques na versão do Android Auto nas configurações do celular) e marcar "Fontes Desconhecidas" (Unknown Sources).
2. **Permissões de Localização em Segundo Plano**:
   - Como o Android Auto roda em tela cheia no carro enquanto a tela do celular pode estar desligada ou em outra Activity, o serviço de localização GPS precisa rodar como um **Foreground Service** (`FOREGROUND_SERVICE_LOCATION` no Android 14+ / API 34).

---

## 7. Arquitetura Proposta para o Velocímetro GPS

```
                        [ GNSS / GPS Hardware ]
                                   │
                                   ▼
                    [ FusedLocationProviderClient ]
                                   │
                                   ▼
                       [ GpsSpeedManager (Service) ]
                                   │
                                   ▼
                         [ SpeedRepository ]
                                   │
                                   ▼
                 [ SpeedFilter (EMA / Kalman Filter) ]
                                   │
                                   ▼
                         [ SpeedState (StateFlow) ]
                                   │
                ┌──────────────────┴──────────────────┐
                ▼                                     ▼
   [ GpsSpeedometerActivity ]             [ AutoSpeedometerActivity ]
      (Interface Smartphone)                  (Interface Android Auto)
                │                                     │
                └──────────────────┬──────────────────┘
                                   ▼
                         [ SpeedometerView ]
                     (Desenho Canvas Customizado)
```
