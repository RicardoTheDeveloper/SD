
#Authors: Ricardo Simões nº2019231869 e Stefanos Panagoulias nº2021115090


Instruções de execução: (Windows)

1--->Extrair o zip
2--->Navegar para a diretoria principal (SD)
3--->Abrir o projeto no IDE
4--->Abrir um terminal para cada componente do projeto (Downloader,Gateway,Cliente e Index Storage Barrel)

Nota: A ordem de execução é importante. Primeiro deve ser executada a Gateway, depois esperamos até que a Gateway esteja pronta, (Gateway is ready on port 1099), depois executamos o Index storage barrel e por fim o downloader e o cliente.
5--->Para executar os ficheiros:

5.1--->Para correr a Gateway: java -cp ".;lib*;src" Gateway <RMI_Registry_Port> <Gateway_IP_Address> <ISB_Machine_IP1> [...<ISB_Machine_IPn>]"

5.2--->Para correr o Index Storage Barrel: java -cp ".;lib\*;src" IndexStorageBarrel <id> <multicast-address> <multicast-port> <rmi-registry-port> <ip host> <ip other machine> <gateway IP> <gateway RMI port>

5.3--->Para correr o Downloader: java -cp "bin;lib/*" Downloader <nackPort> <gatewayIP> <registryPort>

5.4--->Para correr o cliente: java -cp "bin;lib/*" RMIClient <Server_IP> <RMI_Registry_Port>