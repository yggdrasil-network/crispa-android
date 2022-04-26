all:
	cd yggdrasil-extras; \
		gomobile bind -v -target android -tags mobile -o yggdrasil.aar \
		github.com/yggdrasil-network/yggdrasil-go/contrib/mobile \
		github.com/yggdrasil-network/yggdrasil-go/src/config
	mv -f yggdrasil-go/contrib/mobile/yggdrasil.aar yggdrasil.aar;
	mv -f yggdrasil-go/contrib/mobile/yggdrasil-sources.jar yggdrasil-sources.jar;
