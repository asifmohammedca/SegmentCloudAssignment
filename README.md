docker pull asifmohammedca/segmentcloud:offshore-proxy

docker pull asifmohammedca/segmentcloud:ship-proxy

docker network create proxy-network

docker run -d --name offshore-proxy --network proxy-network -p 9090:9090 -p 9091:9091 asifmohammedca/segmentcloud:offshore-proxy

docker run -d --name ship-proxy --network proxy-network -p 8080:8080 asifmohammedca/segmentcloud:ship-proxy
