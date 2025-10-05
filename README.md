# Azure Blob Decryption Function

Función de Azure que desencripta automáticamente archivos subidos a Blob Storage usando AES-256-CBC.

## Descripción

Esta aplicación detecta archivos encriptados en un contenedor de Azure Blob Storage, los desencripta usando una contraseña almacenada en Key Vault, y sube el resultado a otro contenedor. Todas las operaciones quedan registradas en Table Storage.

## Flujo

1. Usuario sube archivo encriptado al contenedor `encrypted-files/`
2. Azure Function se activa automáticamente
3. Obtiene password desde Key Vault
4. Desencripta el archivo
5. Sube archivo desencriptado a `decrypted-files/` con timestamp agregado al nombre
6. Registra operación en Table Storage

## Variables de Entorno

Configura estas variables en Azure Portal → Function App → Configuration:

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `KEY_VAULT_URL` | URL del Key Vault | `https://mi-vault.vault.azure.net/` |
| `ENCRYPTION_SECRET_NAME` | Nombre del secreto | `encryption-password` |
| `DESTINATION_STORAGE_URL` | Storage de destino | `https://destino.blob.core.windows.net` |
| `DESTINATION_CONTAINER` | Contenedor de salida | `decrypted-files` |
| `LOGS_STORAGE_URL` | Storage para logs | `https://logs.blob.core.windows.net` |
| `LOGS_TABLE_NAME` | Tabla de logs | `decryptionlogs` |

## Permisos Necesarios

Habilita **Managed Identity** en tu Function App y asigna:

- **Key Vault**: `Key Vault Secrets User`
- **Storage Destino**: `Storage Blob Data Contributor`
- **Storage Logs**: `Storage Table Data Contributor`

## Despliegue

```bash
mvn clean package
mvn azure-functions:deploy
```

## Desarrollo Local

Crea `local.settings.json`:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "KEY_VAULT_URL": "https://dev-vault.vault.azure.net/",
    "ENCRYPTION_SECRET_NAME": "dev-password",
    "DESTINATION_STORAGE_URL": "https://dev.blob.core.windows.net",
    "DESTINATION_CONTAINER": "decrypted-files-dev",
    "LOGS_STORAGE_URL": "https://dev.blob.core.windows.net",
    "LOGS_TABLE_NAME": "decryptionlogs"
  }
}
```

Ejecutar:
```bash
mvn azure-functions:run
```

## Uso

Sube tu archivo encriptado al contenedor:

```bash
az storage blob upload \
  --account-name <storage-name> \
  --container-name encrypted-files \
  --name mi-archivo-encriptado \
  --file /ruta/local/archivo
```

**Ejemplos de nombres de archivo:**
- `reporte_financiero`
- `documento.pdf`
- `datos.csv`
- `imagen.jpg`

El archivo desencriptado aparecerá en `decrypted-files/` con timestamp agregado al nombre (ej: `reporte_financiero-20241005143025`).

## Logs

Los logs se registran en:
- **Application Insights**: Logs de ejecución
- **Table Storage**: Trazabilidad completa (éxitos y fallos)

Ver logs:
```bash
az storage entity query \
  --account-name <logs-storage> \
  --table-name decryptionlogs
```

## Características

- Soporta archivos de cualquier tamaño (procesamiento en streaming)
- Compatible con formato OpenSSL y formato simple de encriptación
- Agrega timestamp automático a archivos desencriptados para trazabilidad
- Si el archivo original termina en `.enc`, esa extensión se elimina automáticamente
- Preserva la extensión original del archivo (ej: `.pdf`, `.xlsx`, `.jpg`)
- Limpieza automática de archivos temporales

## Troubleshooting

**Error: "Environment variable X is not set"**
- Verifica que todas las variables estén configuradas en Application Settings

**Error: "Failed to retrieve secret"**
- Verifica que Managed Identity tenga permisos en Key Vault

**Error: "Failed to upload blob"**
- Verifica permisos de escritura en Storage Account de destino