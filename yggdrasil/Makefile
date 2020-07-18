GOPATH=$(shell go env GOPATH)
export GO111MODULE = on

all:
	-go get -u golang.org/x/mobile/cmd/gomobile;
	-go get -u github.com/yggdrasil-network/yggdrasil-go;
	-cd $(GOPATH)/src/github.com/yggdrasil-network/yggdrasil-go; \
		go get -v -d ./...; \
		go get -u github.com/yggdrasil-network/yggdrasil-extras; \
		gomobile init; \
		ANDROID=true ./build;
	mv $(GOPATH)/src/github.com/yggdrasil-network/yggdrasil-go/yggdrasil.aar yggdrasil.aar;
	mv $(GOPATH)/src/github.com/yggdrasil-network/yggdrasil-go/yggdrasil-sources.jar yggdrasil-sources.jar;
