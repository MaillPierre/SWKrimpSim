FROM ubuntu:22.04

WORKDIR /SWKrimpSim
ENV SLIM_ROOT=/opt/slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends libgomp1 default-jre-headless && \
    rm -rf /var/lib/apt/lists/*

COPY SlimBinSource-20120607.tar.gz .

RUN mkdir -p "${SLIM_ROOT}" && \
    tar -xzf SlimBinSource-20120607.tar.gz \
        --strip-components=1 \
        -C "${SLIM_ROOT}" && \
    rm -f SlimBinSource-20120607.tar.gz && \
    mkdir -p "${SLIM_ROOT}/data/datasets" && \
    chmod +x "${SLIM_ROOT}/bin/fic" "${SLIM_ROOT}/bin/fic-uint16"

COPY CT-SLIM.sh .
COPY SWPatternsCover.jar .
RUN chmod +x CT-SLIM.sh

ENTRYPOINT ["./CT-SLIM.sh"]
