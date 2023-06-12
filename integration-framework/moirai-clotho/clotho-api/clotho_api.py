from __future__ import print_function
import sys
import json
sys.path.append("./python/")
import openapi_client
from openapi_client.rest import ApiException
from pprint import pprint
from openapi_client.api_client import ApiClient
from openapi_client.configuration import Configuration

# create an instance of the API class

config = Configuration()
config.host="http://ec2-34-239-142-173.compute-1.amazonaws.com:8008"
api_client = ApiClient(configuration=config)
api_instance_query_api = openapi_client.QueryApi(api_client)
api_instance_schema_api = openapi_client.SchemaApi(api_client)
api_instance_graph_api = openapi_client.GraphApi(api_client)
api_instance_ksf_api = openapi_client.KSFApi(api_client)



def getAsJson(data):
    """
    Decodes file and loads it into a valid json package if the input is written in valid json.

    :param file data: File with json information to be read.
    :returns: A json package with the files information if the file was written in valid json
    :rtype: json
    """
    try:
        return json.loads(data)
    except:
        error_log.append("Input can not be parsed as valid json.")
        return None

def validateJsonLD(json_ld_object):
    """
    Connects to the validate ksf endpoint api.

    :param JsonLdObject json_ld_object: A model defined in the open api. Json object to be submitted for validation.
    """
    try:
        # evaluate ksf(Json-LD) requests for any syntactic errors and warnings
        api_response = api_instance_ksf_api.validate_ksf_request(json_ld_object)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling KSFApi->validate_ksf_request: %s\n" % e)

def addVertex(vertex):
    """
    Connects to the insert vertex endpoint api.

    :param Vertex vertex: A model defined in the open api. A vertex of the graph.
    """
    try:
        api_response = api_instance_graph_api.insert_vertex(vertex.label, request_body=vertex.properties_map)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling insert_vertex: %s\n" % e)

def getVertex(label):
    """
    Connects to the get vertices endpoint api.

    :param str label: The label of the vertex.
    """
    try:
        api_response = api_instance_graph_api.get_vertices(label)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_verticies: %s\n" % e)

def getVertexWithCriteria(label, criteria=None):
    """
    Connects to the get vertices endpoint api.

    :param str label: The label of the vertex.
    :param str criteria: A map of key-val pairs to use as search criteria on vertices.
    """
    try:
        api_response = api_instance_graph_api.get_vertices(label, search_criteria=criteria)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_verticies: %s\n" % e)

def deleteVertex(id):
    """
    Connects to the delete vertices endpoint api.

    :param str id: The unique id of the vertex.
    """
    try:
        api_response = api_instance_graph_api.delete_vertices(id)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_verticies: %s\n" % e)

def addEdge(edge):
    """
    Connects to the insert edge endpoint api.

    :param Edge edge: A model defined by the open api.  The edge of the graph
    """
    try:
        api_response = api_instance_graph_api.insert_edge(edge.label, edge.from_vertex_id, edge.to_vertex_id, request_body=edge.properties_map)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling insert_edge: %s\n" % e)


def getEdge(label):
    """
    Connects to the get edges endpoint api.

    :param str label: The label of the edge.
    """
    try:
        api_response = api_instance_graph_api.get_edges(label)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_edges: %s\n" % e)

def getEdgeWithCriteria(label, criteria):
    """
    Connects to the get edges endpoint api.

    :param str label: The label of the edge.
    :param str criteria: A map of key-val pairs to use as search criteria on edges.
    """
    try:
        api_response = api_instance_graph_api.get_edges(label, search_criteria=criteria)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_edges: %s\n" % e)

def deleteEdge(id):
    """
    Connects to the delete edges endpoint api.

    :param str id: The unique id of the edge.
    """
    try:
        api_response = api_instance_graph_api.delete_edges(id)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling delete_edges: %s\n" % e)

def getGraph():
    """
    Connects to the get graph endpoint api.
    """
    try:
        api_response = api_instance_graph_api.get_graph()
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_graph: %s\n" % e)

##TODO
def postGraph(overwrite, graph):
    """
    Connects to the insert graph endpoint api.

    :param bool overwrite:
    """
    try:
        api_response = api_instance_graph_api.insert_graph(overwrite=overwrite, graph=graph)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_graph: %s\n" % e)

def postSchemaEntity(entity):
    """
    Connects to the insert entities endpoint api.

    :param Entity entity: Entity item to add.
    """
    list = [entity]
    try:
        api_response = api_instance_schema_api.insert_entities(entity=list)
        pprint("This is what we are looking at" + api_response)
    except ApiException as e:
        pprint("Exception when calling insert_entities: %s\n" % e)

def getSchemaEntity(name):
    """
    Connects to the get entities endpoint api.

    :param str name: Name value for entities.
    :param str criteria: A map of key-val pairs to use as search criteria on entities.
    """
    try:
        api_response = api_instance_schema_api.get_entities(name=name)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_entities: %s\n" % e)

def postSchemaEvent(event):
    """
    Connects to the insert events endpoint api.

    :param Event event: Event item to add.
    """
    list = [event]
    try:
        api_response = api_instance_schema_api.insert_events(event=list)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_events: %s\n" % e)

def getSchemaEvent(criteria):
    """
    Connects to the get events endpoint api.

    :param str criteria: A map of key-val pairs to use as search criteria on events.
    """
    try:
        api_response = api_instance_schema_api.get_events(search_criteria=criteria)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_events: %s\n" % e)

def getEventbyName(name):
    """
    Connects to the get events by name endpoint api.

    :param str name: Unique name of event to retrieve.
    """
    try:
        api_response = api_instance_schema_api.get_event_by_name(name)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_event_by_name: %s\n" % e)

def postSchema(schema):
    """
    Connects to the insert schema endpoint api.

    :param Schema schema: schema item to add.
    """
    try:
        api_response = api_instance_schema_api.insert_schema(schema=schema)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling insert_schema: %s\n" % e)

def getSchema(criteria):
    """
    Connects to the get schemata endpoint api.

    :param str criteria: A map of key-val pairs to use as search criteria on schemas.
    """
    try:
        api_response = api_instance_schema_api.get_schemata(search_criteria=criteria)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling get_schemata: %s\n" % e)

def passQuery(query):
    """
    Connects to the sparql Query endpoint api.

    :param str query: Query to passthrough item to insert.
    """
    try:
        api_response = api_instance_query_api.sparql_query(sparql_query=query)
        pprint(api_response)
    except ApiException as e:
        pprint("Exception when calling sparql_query: %s\n" % e)

if __name__ == '__main__':
    f = open(sys.argv[1], "r")
    data = f.read()
    json_data = getAsJson(data)

    # The code relates to ksf validation endpoint.
#    json_ld_object = openapi_client.JsonLdObject(at_context=json_data['@context']) # JsonLdObject | KSF submission to validate
#    json_ld_object.sdf_version=json_data['sdfVersion']
#    json_ld_object.schemas=json_data['schemas']
#    json_ld_object.ta2=False
#    validateJsonLD(json_ld_object)


    # Adding vertex
#    vertex = openapi_client.Vertex(label=json_data['label'], properties_map=json_data['propertiesMap'])
#    addVertex(vertex)

    # Getting vertex with label
#    label = json_data['label']
#    getVertex(label)

    # Getting vertex with label and criteria
#    label = json_data['label']
#    criteria = json_data['criteria']
#    getVertexWithCriteria(label, criteria)

    # Deleting vertex
#    id = json_data['id']
#    deleteVertex(id)

    # Adding edge
#    edge = openapi_client.Edge(label=json_data['label'], from_vertex_id=json_data['fromVertexId'], to_vertex_id=json_data['toVertexId'], properties_map=json_data['propertiesMap'])
#    addEdge(edge)

    # Getting edge with label
#    label = json_data['label']
#    getEdge(label)

    # Getting edge with label and criteria
#    label = json_data['label']
#    criteria = json_data['criteria']
#    getEdgeWithCriteria(label, criteria)

    # Deleting edge
#    id = json_data['id']
#    deleteVertex(id)

    # Getting graph
#    getGraph()

    # Add graph
#    graph = openapi_client.Graph(vertex_array=json_data['vertexArray'], edge_array=json_data['edgeArray'])
#    overwrite = json_data['overwrite']
#    postGraph(overwrite, graph)

    # Adding entity
#    entity = openapi_client.Entity(name=json_data['name'])
#    entity.at_id = json_data['@id']
#    entity.id = json_data['id']
#    pprint(entity.name)
#    entity.description = json_data['description']
#    entity.system_error = json_data['systemError']
#    postSchemaEntity(entity)

    # Getting entity
#    name = json_data['name']
#    criteria = json_data['criteria']
#    getSchemaEntity(name)

    # Adding event
#    event = openapi_client.Event(at_id=json_data['@id'], name=json_data['name'])
#    event.version = json_data['version']
#    event.description = json_data['description']
#    postSchemaEvent(event)

    # Getting event
#    criteria = json_data['criteria']
#    getSchemaEvent(criteria)

    # Getting event with name
#    name = json_data['name']
#    getEventbyName(name)

    # Adding schema
#    schema = openapi_client.Event(at_id=json_data['@id'], name=json_data['name'])
#    schema.version = json_data['version']
#    schema.id = json_data['id']
#    postSchema(schema)

    # Getting schema
#    criteria = json_data['criteria']
#    getSchema(criteria)

    # Passthrough sparql query to database
#    query = openapi_client.SparqlQuery(query_string=json_data['queryString'])
#    passQuery(query)