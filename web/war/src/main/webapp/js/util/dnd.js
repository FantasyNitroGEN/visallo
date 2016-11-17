define(['util/vertex/formatters'], function(F) {

    const FALLBACK_PREFIX = 'Visallo_ElementIds: ';

    // IE doesn't support setData([mimetype], ...)
    // if only supports setData('text', ...)
    const checkIfSupportsMultipleTypes = _.once((fn, dataTransfer) => {
        try {
            if (fn === 'set') {
                dataTransfer.setData('CHECK_ALLOWS_MANY_TYPES', 'true')
            } else {
                dataTransfer.getData('CHECK_ALLOWS_MANY_TYPES')
            }
            return true;
        } catch(e) {
            return false;
        }
    })

    return {
        dataTransferHasValidMimeType(dataTransfer, mimeTypes = []) {
            if (checkIfSupportsMultipleTypes('get', dataTransfer)) {
                return _.any(dataTransfer.types, type => mimeTypes.includes(type));
            }
            return true;
        },
        setDataTransferWithElements(dataTransfer, { vertexIds, edgeIds, elements = [] }) {
            const typeToData = segmentToTypes(vertexIds, edgeIds, elements);
            if (checkIfSupportsMultipleTypes('set', dataTransfer)) {
                _.each(typeToData, (data, type) => {
                    if (data) {
                        dataTransfer.setData(type, data);
                    }
                })
            } else {
                dataTransfer.setData('Text', FALLBACK_PREFIX + typeToData[VISALLO_MIMETYPES.ELEMENTS])
            }
        },
        getElementsFromDataTransfer(dataTransfer) {
            var dataStr;
            if (checkIfSupportsMultipleTypes('get', dataTransfer)) {
                dataStr = dataTransfer.getData(VISALLO_MIMETYPES.ELEMENTS);
            } else {
                const text = dataTransfer.getData('Text');
                if (text.indexOf(FALLBACK_PREFIX) === 0) {
                    dataStr = text.substring(FALLBACK_PREFIX.length);
                }
            }

            if (dataStr) {
                return JSON.parse(dataStr);
            }
        }
    }

    function segmentToTypes(vertexIds = [], edgeIds = [], elements) {
        const hasFullElements = elements.length > 0;
        if (hasFullElements) {
            vertexIds = [];
            edgeIds = [];
            elements.forEach(({ id, type }) => {
                if (type === 'vertex') {
                    vertexIds.push(id);
                } else {
                    edgeIds.push(id);
                }
            })
        }
        const url = F.vertexUrl.url(hasFullElements ? elements : vertexIds.concat(edgeIds), visalloData.currentWorkspaceId);
        const plain = hasFullElements && elements.map(item => [
                F.vertex.title(item), F.vertexUrl.url([item], visalloData.currentWorkspaceId)
            ].join('\n')).join('\n\n') || null;

        return {
            'text/uri-list': url,
            'text/plain': plain,
            [VISALLO_MIMETYPES.ELEMENTS]: JSON.stringify({ vertexIds, edgeIds })
        }
    }
})
