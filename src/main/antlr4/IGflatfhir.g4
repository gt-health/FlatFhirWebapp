grammar IGflatfhir;

document: '{'entries'}';
entries: (igmap|profilemap|datamap) (',' (igmap|profilemap|datamap))* ;

/** ig_map definitions */
igmap: '"' KEYNAME '": {' (igentry)(',' igentry)* '}';
igentry: (version|fhirversion|localdataset|url);
version: '"version": "' VALUE '"';
fhirversion: '"fhir_version": "' VALUE '"';
localdataset: '"local_dataset": "' VALUE '"';
url: '"url": "' VALUE '"';

/** top-level profile definitions. Entries can be maps or lists, and link to other maps or lists */
profilemap: '"profiles": {' (profilemapentry|profilelistentry)(',' profilemapentry|profilelistentry) '}';
profilemapentry: '"' KEYNAME '": {' (localdefentry|codeableconceptentry|profilemapentry)(',' localdefentry|codeableconceptentry|profilemapentry)* '}';
profilelistentry: '"' KEYNAME '": [' (profilemapentry|profilelistentry)(',' profilemapentry|profilelistentry)* ']';

/** child entries in profiles. Sets local def from the data section, and special code value mappings */
localdefentry: '"local_def": "' VALUE '"';
codeableconceptentry: '"' KEYNAME '": {' (systementry|codeentry|displayentry)(',' systementry|codeentry|displayentry)* '}';
systementry: '"system": "' VALUE '"';
codeentry: '"code": "' VALUE '"';
displayentry: '"display": "' VALUE '"';

/** top-level data definitions. Where the actual data gets mapped */
datamap: '"data": {'(dataentry)(',' dataentry)* '}';
dataentry: (simpledata|dataobject);
simpledata: '"' KEYNAME '":' VALUE;
dataobject: '"' KEYNAME '": {'(dataentry)(',' dataentry)* '}';

fragment LOWERCASE: [a-z] ;
fragment UPPERCASE: [A-Z] ;
fragment NUMBER: [0-9];
fragment SPECIALCHAR: [-:/._!@#$%^&*()];
fragment SPACECHARS: [ \t\r\n];
fragment EMPTYSTRING: '""';
KEYNAME: [a-zA-Z\-_]+;
VALUE: (LOWERCASE | UPPERCASE | NUMBER | SPECIALCHAR | EMPTYSTRING)+;
WHITESPACE: (SPACECHARS)+ -> skip;