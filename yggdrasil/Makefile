all:
	cd yggdrasil-extras; \
		gomobile bind -v -target android -tags mobile -o yggdrasil.aar \
		github.com/yggdrasil-network/yggdrasil-extras/src/mobile \
		github.com/yggdrasil-network/yggdrasil-go/src/config
	mv -f yggdrasil-extras/yggdrasil.aar yggdrasil.aar;
	mv -f yggdrasil-extras/yggdrasil-sources.jar yggdrasil-sources.jar;
