# Build everything

```
VERSION=<version number> ./prepare.sh
```

## Reset submodules
```
git submodule deinit -f .                                                   
git submodule update --init
```

## Update submodules to the latest main commit ##
```
git submodule foreach git pull origin main
```
