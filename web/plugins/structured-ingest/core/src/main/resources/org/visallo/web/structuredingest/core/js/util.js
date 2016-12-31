define([
    'util/withDataRequest'
],
function(withDataRequest) {
    'use strict';

    var analyze = _.memoize(function(vertexId) {
        return withDataRequest.dataRequest('org-visallo-structuredingest', 'analyze', vertexId)
    })

    return {

        CONCEPT_TYPE: 'http://visallo.org#conceptType',

        findPropertiesForColumnInObject: function(object, columnIndex) {
            return _.filter(object.properties, function(p) {
                if (p.column === columnIndex) return true;
                if (p.hints && (
                    columnIndex === p.hints.columnLatitude ||
                    columnIndex === p.hints.columnLongitude)) {
                    return true;
                }
            });
        },

        analyze: function(vertexId, options) {
            return analyze(vertexId)
                .then(function(result) {
                    var sheet = (options && 'sheetIndex' in options) ?
                            result.sheets[options.sheetIndex] :
                            _.first(result.sheets),
                        allRows = (sheet || []).parsedRows.map(function(r, i) {
                            r.index = i;
                            r.isBlank = _.every(r.columns, _.isEmpty)
                            return r;
                        }),
                        headerIndex = options && options.headerIndex || 0,
                        rows = _.reject(allRows.slice(headerIndex), function(r) {
                            return r.isBlank;
                        }),
                        longestColumn = _.max(rows, function(r) {
                            return r.columns.length;
                        }).columns.length,
                        headers;

                    if (options && options.maxRows) {
                        rows = rows.slice(0, options.maxRows + 1);
                    }

                    if (options && options.addBlankLastRow) {
                        rows.push({ isBlankLastRow: true, columns: [] })
                    }

                    rows.forEach(function(row, i) {
                        while (row.columns.length < longestColumn) {
                            row.columns.push(' ');
                        }
                    })

                    if (!_.isEmpty(sheet.columns)) {
                        headers = _.pluck(sheet.columns, 'name');
                    } else if (options && options.hasHeaderRow === false) {
                        headers = _.range(longestColumn).map(function(i) {
                            return 'Column ' + (i + 1);
                        })
                    } else {
                        headerIndex = rows[0].index;
                        headers = rows && rows[0].columns || [];
                        rows.splice(0, 1);
                    }

                    return {
                        headerIndex: headerIndex,
                        headers: headers,
                        headerTypes: _.object(sheet.columns.map(function(c) {
                            return [c.name, c.type]
                        })),
                        rows: rows,
                        total: sheet.totalRows,
                        hints: result.hints,
                        sheets: _.pluck(result.sheets, 'name')
                    }
                });
        }
    }
})
