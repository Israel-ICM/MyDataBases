# Estándares de Testing

## Objetivo de Cobertura

**Mínimo 80% de cobertura de código** para:

- Capa domain (casos de uso, repositorios)
- Capa data (implementaciones de repositorio)
- ViewModels

Tests de UI se enfocan en rutas críticas, no en 100% de cobertura.

## Tipos de Tests

### Unit Tests

Testear componentes individuales en aislamiento.

**Ubicación**: `src/test/`

**Framework**: JUnit 5 + Mockk

```kotlin
class FetchUserUseCaseTest {
    
    private val repository: UserRepository = mockk()
    private val useCase = FetchUserUseCase(repository)
    
    @Test
    fun `fetchUser retorna success cuando repository tiene éxito`() = runTest {
        // Given
        val userId = "123"
        val expectedUser = User(id = userId, name = "Juan")
        coEvery { repository.getUser(userId) } returns Result.success(expectedUser)
        
        // When
        val result = useCase(userId)
        
        // Then
        assertEquals(Result.success(expectedUser), result)
    }
}
```

### Integration Tests

Testear múltiples componentes trabajando juntos.

**Ubicación**: `src/test/`

**Usar implementaciones reales cuando sea posible**, mocks solo para sistemas externos.

### UI Tests

Testear flujos de usuario end-to-end.

**Ubicación**: `src/androidTest/`

**Framework**: Compose UI Testing

```kotlin
class AuthScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun loginConCredencialesValidas_navegaAHome() {
        composeTestRule.setContent {
            AuthScreen(onLoginSuccess = { /* navegar */ })
        }
        
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Contraseña").performTextInput("password123")
        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()
        
        // Assert navegación o cambio de estado
    }
}
```

## Nomenclatura de Tests

Usar nombres descriptivos que expliquen el escenario:

```
nombreFuncion_estadoBajoTest_comportamientoEsperado
```

Ejemplos:

- `fetchUser_cuandoRepositoryFalla_retornaError`
- `botonLogin_cuandoCamposVacios_estaDeshabilitado`
- `deleteConnection_cuandoConfirmado_eliminaDeLista`

## Estructura de Tests

Usar patrón **Given-When-Then**:

```kotlin
@Test
fun test() {
    // Given (Arrange)
    val input = "test"
    
    // When (Act)
    val result = function(input)
    
    // Then (Assert)
    assertEquals(expected, result)
}
```

## Mocking

- **Preferir fakes sobre mocks** cuando sea posible
- **Mockear dependencias externas** (APIs, bases de datos)
- **Usar implementaciones reales** para lógica de dominio

```kotlin
// Fake
class FakeUserRepository : UserRepository {
    private val users = mutableListOf<User>()
    
    override suspend fun getUser(id: String): Result<User> =
        users.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(NotFoundException())
}

// Mock (cuando fake es impráctico)
val api: ApiService = mockk()
coEvery { api.getUser(any()) } returns UserDto(/* ... */)
```

## Testing de Coroutines

Usar `runTest` de `kotlinx-coroutines-test`:

```kotlin
@Test
fun coroutineTest() = runTest {
    val result = suspendingFunction()
    assertEquals(expected, result)
}
```

## Organización de Tests

```
src/test/
├── domain/
│   └── usecases/
├── data/
│   └── repositories/
└── presentation/
    └── viewmodels/

src/androidTest/
└── ui/
    └── screens/
```

---

*Escribir tests JUNTO con el código, no después.*
