# K8S manifests

## NGINX Ingress controller nginx

See https://kubernetes.github.io/ingress-nginx.

if you use a managed K8S cluster you may want to look at using an ingress
 controller specific to your provider.

Setup the controller in your cluster.
```bash
kubectl apply -f ingress-nginx.yaml
```

TIP: look at the logs when troubleshooting or event exec in the pod's container
to look at `nginx.conf`

## NFS storage class provider

See https://github.com/kubernetes-incubator/external-storage/tree/master/nfs-client.

This is for testing purpose only, if you use a managed K8S cluster you should
 not follow this step.

Edit the file to change to configure your NFS server hostname (`your-nfs-host`)
 and the shared path (`/your/shared/path`).

Setup a storage class provider:
```bash
kubectl apply -f storage-class-nfs.yaml
```

### Persistent volume claim

The deployment pod template reference a claim called `build-publisher-storage`.

If you use a managed K8S cluster you should follow your providers documentation
 and create a PVC called `build-publisher-storage`.

Create a PVC with the NFS storage class provider (testing only):
```bash
kubectl apply -f storage-claim-nfs.yaml
```

### Publisher backend

Create a PKCS8 key pair:
```bash
openssl genpkey -out backend.pem -algorithm RSA -pkeyopt rsa_keygen_bits:2048
openssl rsa -in backend.pem -pubout > backend.pem.pub
```

Create a secret containing the public key:
```bash
kubectl create secret generic backend-secret --from-file=backend.pem.pub
```

Create a deployment and service:
```bash
kubectl apply -f backend.yaml
```

IMPORTANT: the backend is not designed to scale.

This is because the storage is file system based the backend is designed to be
 a single instance in order to avoid dealing with concurrent write access.

Create an ingress resource (testing only):
```bash
kubectl apply -f backend-ingress-localhost.yaml
```

You will need to create your own version of this to match your desired hostname
 and use ingress controller specific annotations for URL re-write.

### Publisher frontend

Create a deployment and service:
```bash
kubectl apply -f frontend.yaml
```

NOTE: the frontend can be scaled since it's only doing read access to the storage.

Create an ingress resource (testing only):
```bash
kubectl apply -f frontend-ingress-localhost.yaml
```

You will need to create your own version of this to match your desired hostname
 and use ingress controller specific annotations for the URL re-write.