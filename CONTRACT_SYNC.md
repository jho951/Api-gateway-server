# Contract Sync (Api-gateway-server)

- Contract Source: https://github.com/jho951/contract
- Service SoT Branch: `main`
- Contract Role: Edge routing and header propagation owner

## Required Links
- Routing: https://github.com/jho951/contract/blob/main/contracts/routing.md
- Headers: https://github.com/jho951/contract/blob/main/contracts/headers.md
- Security: https://github.com/jho951/contract/blob/main/contracts/security.md
- Env: https://github.com/jho951/contract/blob/main/contracts/env.md

## Sync Checklist
- [ ] `/v1/**` route mapping matches contract
- [ ] stripPrefix(`/v1`) behavior unchanged
- [ ] INTERNAL guard (`X-Internal-Request-Secret`) enforced
- [ ] Trace headers (`X-Request-Id`, `X-Correlation-Id`) propagated
- [ ] 502/504 mapping follows contract
