O Covid  é baseado no visualizador Weasis em Java

![Weasis](https://user-images.githubusercontent.com/993975/39397039-2180c178-4af9-11e8-9c72-2c1e9aa16eae.jpg)

* [Informações sobre o Weasis](https://nroduit.github.io)

* [Issues](https://github.com/nroduit/Weasis/issues) ([Old Issue Tracker](https://dcm4che.atlassian.net/projects/WEA))

# Instalando Ambiente de Desenvolvimento
``` bash
echo "Instalando Java, Maven e Eclipse para Ubuntu 16.06"
sudo sh install-dev-env.sh
```

# Build Weasis

O Visualizador é baseando no  Weasis 3.x.x (Necessitando de  Java 8+)

Documentação [Oficial do Weasis](https://nroduit.github.io/en/getting-started/building-weasis)
``` bash
echo "Clonando o Projeto"
git clone https://github.com/rtmedical/covid
cd covid/viewer/
echo "Rodando o Maven"
mvn clean install
echo "Construindo o Aplicativo"
cd weasis-distributions
mvn clean package -Dportable=true -P pack200
 ```


# Executando Aplicativo Gerado
Acesse a pasta covid/viewer/weasis-distributions/target/portable-dist/
Nesta Pasta, você poderá executar o aplicativo para Linux, Windows e MAC.

