# Despliegue del BFF con AWS Academy

Este pipeline evita OIDC, creación de roles IAM, Systems Manager y publicación en ECR. GitHub ejecuta las pruebas en un runner hospedado; cuando un cambio del backend llega a `main`, un runner autohospedado en la instancia EC2 construye la imagen Docker y reemplaza localmente el contenedor `vidasalud-bff`.

## Qué configurar en GitHub

No hacen falta secretos ni variables de AWS para este flujo. En el repositorio, abre **Settings → Actions → Runners → New self-hosted runner**, selecciona **Linux** y **x64**, y sigue las instrucciones que GitHub genera para registrar el runner en la instancia EC2. Esas instrucciones incluyen un token temporal de registro: úsalo para instalar el runner y no lo guardes en el repositorio.

Configura el runner con la etiqueta personalizada `vidasalud-ec2` y verifica en **Settings → Actions → Runners** que aparezca **Online**. Instálalo como servicio bajo `ec2-user` o un usuario dedicado sin privilegios de root, y asegúrate de que ese usuario pueda usar Docker. La instancia necesita Docker activo, `curl` y acceso a Internet para descargar las imágenes base del Dockerfile. Java y Maven no son necesarios en EC2; las pruebas se ejecutan en GitHub.

## Flujo

- Los pull requests que afectan al BFF ejecutan `Test BFF` en `ubuntu-latest`; no usan el runner de producción.
- Solo un push a `main` que cambie `ms-vidasalud-bff/` puede iniciar el despliegue.
- El despliegue espera a que terminen correctamente las pruebas, construye una imagen local etiquetada con el SHA del commit y verifica `/actuator/health`.
- El script conserva el entorno, puertos, redes y política de reinicio del contenedor actual. Si el contenedor nuevo no queda saludable, restaura el anterior.
- El contenedor previo se conserva detenido con un nombre `vidasalud-bff-rollback-<sha>` para permitir rollback manual; no se elimina automáticamente.

## Seguridad y disponibilidad

El runner tiene acceso a Docker en el host de producción. Protege `main`, revisa los cambios antes de integrarlos y no ejecutes workflows de pull requests no confiables en este runner. Los jobs de PR definidos aquí usan exclusivamente runners hospedados por GitHub.

La instancia EC2 y el entorno/laboratorio de AWS Academy deben permanecer encendidos y conectados a GitHub. Si AWS Academy detiene la instancia, el runner queda offline y el job de despliegue esperará hasta agotar el límite de 20 minutos. Al reanudar el laboratorio, el runner volverá a conectarse; el despliegue no ocurre mientras la instancia esté apagada.

Esta ruta crea la imagen en EC2 y no la publica en ECR. El permiso de solo lectura de ECR de `LabRole` no se usa para el despliegue.

## Activar sin desplegar accidentalmente

Primero instala y verifica el runner con la etiqueta `vidasalud-ec2`. Después integra este workflow. Su push inicial no dispara el job de despliegue porque el filtro de `main` solo observa cambios del backend y excluye el archivo de protección `.dockerignore`. El despliegue automático comenzará con el siguiente cambio de código del BFF integrado en `main`.

El script `scripts/deploy-bff-ec2-runner.sh` también se puede ejecutar en EC2 con `GITHUB_SHA` definido, pero normalmente lo ejecuta GitHub Actions.
