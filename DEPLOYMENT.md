# WhatTheBus OCI Deployment Guide

This guide explains how to deploy the WhatTheBus Spring Boot service with an Nginx reverse proxy and Let's Encrypt TLS certificates on an OCI Free Tier compute instance.

## 1. Prerequisites
- OCI compute instance (Oracle Linux/Ubuntu) with Docker Engine and Docker Compose Plugin installed.
- DuckDNS domain pointing to the instance's public IP.
- Security lists / network security groups allowing inbound TCP 80/443 and UDP 5684.
- OCI Vault configured with the secrets referenced by application-prod.properties.

## 2. Prepare the repository
`
# On your local machine
./gradlew test
`
Ensure the tests pass. Commit any changes before building images.

## 3. Build and push the application image
`
# Local build
docker compose build app

# (Optional) tag & push to OCI registry
REGION=ap-chuncheon-1
OCIR_NS=<your_tenancy_namespace>
REPO=whatthebus
TAG=v1

docker tag whatthebus-app:latest .ocir.io//:
docker login .ocir.io
# (Follow OCI CLI instructions for auth token)
docker push .ocir.io//:
`
On the compute instance you can either pull the pushed image or rebuild from source.

## 4. Configure environment files
deploy/env/app.env is optional and meant for non-secret overrides (e.g. JAVA_OPTS). Leave it empty if all secrets come from OCI Vault.

Prep nginx config:
- Copy deploy/nginx/conf.d/whatthebus.conf.sample to deploy/nginx/conf.d/whatthebus.conf.
- Replace <YOUR_DOMAIN>.duckdns.org with the actual DuckDNS hostname.
- Adjust timeouts or logging paths if necessary.

## 5. Issue the initial Let's Encrypt certificate
On the OCI instance:
`
# Clone repo or pull image bundle
cd /opt/whatthebus

# Start application + nginx (certbot profile excluded)
docker compose up -d app nginx

# Obtain certificate (replace domain/email)
docker compose run --rm \
  --entrypoint "certbot certonly --webroot -w /var/www/certbot \
  --email you@example.com --agree-tos --no-eff-email \
  -d <YOUR_DOMAIN>.duckdns.org" certbot

# Reload nginx to use the new cert
docker compose exec nginx nginx -s reload
`
Make sure the challenge succeeds (DuckDNS A record must point to the instance).

## 6. Enable HTTPS and application service
After certificates are in place, restart the stack to ensure nginx picks them up on container start:
`
docker compose down
docker compose up -d
`
Visit https://<YOUR_DOMAIN>.duckdns.org to confirm SSL and application health.

## 7. Automate certificate renewal
Create a cron job on the host (e.g. /etc/cron.d/certbot-renew):
`
0 3 * * * root cd /opt/whatthebus && docker compose run --rm certbot certbot renew --quiet && docker compose exec nginx nginx -s reload
`
This attempts renewal daily at 03:00 and reloads nginx if certificates change.

## 8. Operations tips
- docker compose logs -f nginx / apppp for troubleshooting.
- To update the app: pull latest source, rebuild docker compose build app, then docker compose up -d app nginx.
- Keep OCI Vault secrets in sync with any new configuration keys.

## 9. Cleanup
To remove the stack:
`
docker compose down
`
Add -v if you wish to delete the stored certificates (certbot-www, letsencrypt volumes).
