GOPATH=$(shell go env GOPATH)

all:
	-go get -u github.com/yggdrasil-network/yggdrasil-go;
	-cd $(GOPATH)/src/github.com/yggdrasil-network/yggdrasil-go; \
		go get -v -d ./...; \
		go get -u github.com/yggdrasil-network/yggdrasil-extras@005d79c; \
		ANDROID=true ./build;
	mv -f $(GOPATH)/src/github.com/yggdrasil-network/yggdrasil-go/yggdrasil.aar yggdrasil.aar;
	mv -f $(GOPATH)/src/github.com/yggdrasil-network/yggdrasil-go/yggdrasil-sources.jar yggdrasil-sources.jar;
