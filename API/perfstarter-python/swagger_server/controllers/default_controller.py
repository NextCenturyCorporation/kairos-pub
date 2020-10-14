import connexion
import six

from swagger_server.models.entrypoint_message import EntrypointMessage  # noqa: E501
from swagger_server.models.entrypoint_response import EntrypointResponse  # noqa: E501
from swagger_server.models.status_response import StatusResponse  # noqa: E501
from swagger_server import util


def kairos_entrypoint_post(body):  # noqa: E501
    """Performer algorithm execution entry point.

    Kicks off the performer algorithm execution. # noqa: E501

    :param body: Contains the message payload to be passed in to the performer algorithm. id, sender, and time fields will always be populated. 
* The message will container either the &#x27;content&#x27; field or &#x27;contentUri&#x27; field but not both. 
* If content is set it will contain the data to be processed.
* If &#x27;contentUri&#x27; is set it will contain the address to find the content which may either be hosted remotely or as a local file.

    :type body: dict | bytes

    :rtype: EntrypointResponse
    """
    if connexion.request.is_json:
        body = EntrypointMessage.from_dict(connexion.request.get_json())  # noqa: E501
    return 'do some magic!'


def kairos_status_get():  # noqa: E501
    """Returns current processing state of performer container.

    status field must be one of the \&quot;statusTypes\&quot; listed below. # noqa: E501


    :rtype: StatusResponse
    """
    return 'do some magic!'
