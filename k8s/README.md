## Common K8s files

Setup an ingress controller based on NGINX:
```bash
kubectl -f apply ingress.yaml
```
TIP: Look at the logs when troubleshooting or event exec in the pod's container
to look at `nginx.conf`

Setup a storage class that allocates PVC from a NFS file system:

Edit the file to change to configure your NFS server hostname (`your-nfs-host`)
 and the shared path (`/your/shared/path`).

```bash
kubectl apply -f nfs-storage-provider.yaml
```

Create a persistent storage claim:
```bash
kubectl apply -f storage-claim.yaml
```