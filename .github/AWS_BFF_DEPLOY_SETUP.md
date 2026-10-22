# Configuración inicial del despliegue automático del BFF

El workflow `.github/workflows/bff-ci-cd.yml` ejecuta pruebas para los PR que cambian `ms-vidasalud-bff/`. Después del merge a `main`, construye una imagen `linux/amd64`, la publica en ECR con el SHA del commit y despliega ese digest en EC2 mediante Systems Manager (SSM). El script conserva variables de entorno, puertos, redes Docker y política de reinicio del contenedor actual; verifica `/actuator/health` y restaura el contenedor previo si el nuevo no queda saludable.

## Variables del repositorio GitHub

En **Settings → Secrets and variables → Actions → Variables**, configura:

| Variable | Valor |
| --- | --- |
| `AWS_REGION` | `us-east-1` |
| `AWS_ACCOUNT_ID` | ID de cuenta AWS que contiene ECR y EC2 |
| `ECR_REPOSITORY` | `ms-vidasalud-bff` |
| `BFF_EC2_INSTANCE_ID` | ID de la instancia EC2 que ejecuta `vidasalud-bff` |
| `AWS_DEPLOY_ROLE_ARN` | ARN del rol que asumirá GitHub Actions vía OIDC |

No guardes contraseñas de PostgreSQL/RabbitMQ en GitHub Actions: el workflow reutiliza el entorno configurado en el contenedor de EC2.

## Requisitos AWS

1. Configura el proveedor OIDC de GitHub (`token.actions.githubusercontent.com`) en IAM.
2. Crea un rol confiable solo para `repo:Joscelynne/VidaSalud:ref:refs/heads/main` (audience `sts.amazonaws.com`). Así, los PR y otras ramas no pueden asumir el rol de despliegue.
3. Adjunta al rol una política mínima para obtener credenciales ECR y publicar imágenes en el repositorio `ms-vidasalud-bff` (`ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`), más `ssm:SendCommand` limitado al documento `AWS-RunShellScript` y a la instancia objetivo, y `ssm:GetCommandInvocation` para verificar el resultado.
4. Asegura que la instancia tenga un instance profile con `AmazonSSMManagedInstanceCore` y lectura del repositorio ECR (`ecr:GetAuthorizationToken`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`).
5. En EC2 deben estar instalados y activos SSM Agent, Docker, AWS CLI y `curl`; el usuario que ejecuta SSM debe poder ejecutar Docker. La instancia necesita salida HTTPS a ECR y Systems Manager.
6. Verifica que el contenedor actual se llame `vidasalud-bff` y que `/actuator/health` esté accesible en `127.0.0.1:8080`. Si difiere, ajusta `BFF_CONTAINER_NAME` o `BFF_HEALTH_URL` dentro de `scripts/deploy-bff-ssm.sh` antes de habilitar el workflow.

## Comportamiento y seguridad

- Un PR ejecuta pruebas, pero no obtiene credenciales AWS ni despliega.
- El despliegue solo ocurre al integrar cambios del BFF en `main`.
- Las imágenes se identifican por digest inmutable; los despliegues no dependen de `latest`.
- Los despliegues se serializan para evitar dos reemplazos simultáneos.
- El contenedor anterior queda detenido como `vidasalud-bff-rollback-<sha>` para rollback manual. No se elimina automáticamente.
- La primera ejecución fallará antes de desplegar si faltan variables/rol IAM; no se debe habilitar hasta completar los requisitos AWS.

## Verificación

Abre **Actions → BFF CI/CD**. Un PR debe completar `Test BFF`. Tras mergear un cambio del backend a `main`, deben completarse `Test BFF` y `Build and deploy BFF to AWS`; el segundo job muestra el resultado del health check y el digest publicado.
