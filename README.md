# Azure PGP File Decryption Function

Función de Azure que desencripta automáticamente archivos PGP subidos a Blob Storage usando BouncyCastle.

## Descripción

Esta aplicación detecta archivos PGP encriptados en un contenedor de Azure Blob Storage, los desencripta usando una clave privada almacenada en Azure Key Vault, y sube el resultado a otro contenedor. Todas las operaciones quedan registradas en Azure Table Storage.

## Arquitectura

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Blob Storage  │───▶│  Azure Function  │───▶│  Blob Storage   │
│ encrypted-files │    │  (PGP Decrypt)   │    │ decrypted-files │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │  Azure Key Vault  │
                       │ (Private Key +    │
                       │  Passphrase)      │
                       └──────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │ Azure Table Storage│
                       │   (Logs)          │
                       └──────────────────┘
```

## Flujo de Trabajo

1. **Trigger**: Usuario sube archivo PGP al contenedor `encrypted-files/`
2. **Autenticación**: Azure Function obtiene credenciales desde Azure Key Vault usando Managed Identity
3. **Desencriptación**: Usa BouncyCastle para desencriptar el archivo PGP con la clave privada
4. **Almacenamiento**: Sube el archivo desencriptado a `decrypted-files/`
5. **Logging**: Registra la operación (éxito/fallo) en Azure Table Storage
6. **Limpieza**: Elimina archivos temporales automáticamente

## Variables de Entorno

Configura estas variables en Azure Portal → Function App → Configuration:

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `KEY_VAULT_URL` | URL del Key Vault | `https://mi-vault.vault.azure.net/` |
| `PGP_PRIVATE_KEY_SECRET_NAME` | Nombre del secreto con la clave privada (Base64) | `pgp-private-key` |
| `PGP_PASSPHRASE_SECRET_NAME` | Nombre del secreto con la passphrase | `pgp-passphrase` |
| `DESTINATION_STORAGE_URL` | Storage de destino | `https://destino.blob.core.windows.net` |
| `DESTINATION_CONTAINER` | Contenedor de salida | `decrypted-files` |
| `LOGS_STORAGE_URL` | Storage para logs | `https://logs.blob.core.windows.net` |
| `LOGS_TABLE_NAME` | Tabla de logs | `decryptionlogs` |

## Permisos Necesarios

Habilita **Managed Identity** en tu Function App y asigna:

- **Key Vault**: `Key Vault Secrets User`
- **Storage Destino**: `Storage Blob Data Contributor`
- **Storage Logs**: `Storage Table Data Contributor`

## Configuración de Key Vault

### 1. Crear Secretos en Key Vault

```bash
# Clave privada PGP (debe estar en formato Base64)
az keyvault secret set \
  --vault-name "mi-vault" \
  --name "pgp-private-key" \
  --value "$(base64 -i private-key.asc)"

# Passphrase de la clave privada
az keyvault secret set \
  --vault-name "mi-vault" \
  --name "pgp-passphrase" \
  --value "tu-passphrase-secreta"
```

### 2. Configurar Acceso

```bash
# Asignar permisos a la Function App
az keyvault set-policy \
  --name "mi-vault" \
  --object-id <function-app-managed-identity-id> \
  --secret-permissions get list
```

## Despliegue

```bash
# Compilar el proyecto
mvn clean package

# Desplegar a Azure
mvn azure-functions:deploy
```

## Desarrollo Local

### 1. Crear `local.settings.json`:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "KEY_VAULT_URL": "https://dev-vault.vault.azure.net/",
    "PGP_PRIVATE_KEY_SECRET_NAME": "pgp-private-key",
    "PGP_PASSPHRASE_SECRET_NAME": "pgp-passphrase",
    "DESTINATION_STORAGE_URL": "https://dev.blob.core.windows.net",
    "DESTINATION_CONTAINER": "decrypted-files-dev",
    "LOGS_STORAGE_URL": "https://dev.blob.core.windows.net",
    "LOGS_TABLE_NAME": "decryptionlogs"
  }
}
```

### 2. Ejecutar localmente:

```bash
mvn azure-functions:run
```

## Uso

### Subir archivo PGP para desencriptar:

```bash
az storage blob upload \
  --account-name <storage-name> \
  --container-name encrypted-files \
  --name documento-secreto.pgp \
  --file /ruta/local/documento-secreto.pgp
```

### Formatos de archivo soportados:
- `.pgp` - Archivos PGP estándar
- `.gpg` - Archivos GPG (compatible con PGP)
- `.enc` - Archivos encriptados (se elimina la extensión)

**Ejemplos:**
- `reporte_financiero.pgp` → `reporte_financiero`
- `documento.pdf.gpg` → `documento.pdf`
- `datos.csv.enc` → `datos.csv`

## Monitoreo y Logs

### Application Insights
- Logs de ejecución en tiempo real
- Métricas de rendimiento
- Alertas automáticas

### Azure Table Storage
- Trazabilidad completa de operaciones
- Registro de éxitos y fallos
- Información de archivos procesados

### Consultar logs:

```bash
# Ver logs de desencriptación
az storage entity query \
  --account-name <logs-storage> \
  --table-name decryptionlogs \
  --query "[?PartitionKey=='SUCCESS']"

# Ver errores
az storage entity query \
  --account-name <logs-storage> \
  --table-name decryptionlogs \
  --query "[?PartitionKey=='FAILURE']"
```

## Características Técnicas

- **Algoritmo**: PGP con BouncyCastle
- **Procesamiento**: Streaming para archivos grandes
- **Seguridad**: Credenciales almacenadas en Azure Key Vault
- **Escalabilidad**: Azure Functions con auto-scaling
- **Tolerancia a fallos**: Manejo robusto de errores
- **Limpieza automática**: Archivos temporales eliminados automáticamente
- **Integridad**: Verificación de integridad de archivos PGP

## Troubleshooting

### Errores Comunes

**"Environment variable X is not set"**
- ✅ Verifica que todas las variables estén configuradas en Application Settings
- ✅ Revisa el archivo `local.settings.json` para desarrollo local

**"Failed to retrieve secret from Key Vault"**
- ✅ Verifica que Managed Identity tenga permisos `Key Vault Secrets User`
- ✅ Confirma que los nombres de secretos coincidan exactamente
- ✅ Verifica que la clave privada esté en formato Base64

**"No matching secret key found or wrong passphrase"**
- ✅ Verifica que la clave privada corresponda al archivo encriptado
- ✅ Confirma que la passphrase sea correcta
- ✅ Asegúrate de que el archivo esté encriptado con la clave pública correspondiente

**"Failed to upload blob"**
- ✅ Verifica permisos `Storage Blob Data Contributor` en el storage de destino
- ✅ Confirma que el contenedor `decrypted-files` exista
- ✅ Verifica conectividad de red

**"Message failed integrity check"**
- ✅ El archivo PGP puede estar corrupto
- ✅ Verifica que el archivo no haya sido modificado después del encriptado

### Logs de Debug

Para habilitar logs detallados, agrega en `local.settings.json`:

```json
{
  "Values": {
    "AZURE_FUNCTIONS_ENVIRONMENT": "Development",
    "FUNCTIONS_WORKER_RUNTIME": "java"
  }
}
```

## Estructura del Proyecto

```
src/main/java/org/sky/
├── azure/                    # Clientes de Azure
│   ├── AzureBlobStorageDecrypt.java
│   ├── AzureCredentialsProvider.java
│   ├── AzureKeyVaultClient.java
│   └── AzureTableStorageClient.java
├── function/                 # Azure Function principal
│   ├── BlobDecryptFunction.java
│   ├── DecryptionConfig.java
│   └── exception/
│       ├── DecryptionException.java
│       └── KeyVaultException.java
├── model/                    # Modelos de datos
│   ├── DecryptionKeys.java
│   ├── DecryptionLog.java
│   └── KeyAndIV.java
└── utils/                    # Utilidades de desencriptación
    ├── FileDecryptor.java    # Para AES (no usado actualmente)
    └── PGPFileDecryptor.java # Para PGP (implementación actual)
```

## Dependencias Principales

- **Azure Functions Java Library**: 3.0.0
- **BouncyCastle PGP**: 1.78.1
- **Azure Identity**: 1.7.0
- **Azure Key Vault Secrets**: 4.5.0
- **Azure Storage Blob**: 12.19.0
- **Azure Data Tables**: 12.3.0