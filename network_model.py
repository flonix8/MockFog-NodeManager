"""
First prototype for the network model. The network model can be 
deployed on the nodes or queried by the frontend to get the
information to be displayed.
"""

import networkx

nodes = [
    ip_adress('1.1.1.1'),
    ip_adress('1.1.1.2'),
    ip_adress('1.2.1.1')
]

edges = [
    (0,1),
    (0,2)
]

delays = [ # in ms
    100,
    150
]

rates = [
    '100K',
    '100K'
]

_config_template = {
    'dev': 'eth0',
    'out_rate': '100kbps',
    'in_rate': '10kbps',
    'rules': [
        {
            'dst_net': '0.0.0.0',
            'delay': '100ms',
            'dispersion': '10ms'
        }
    ] 
}

import networkx as nx
g = nx.Graph(edges)
for idx, edge in enumerate(edges):
    g.edges[edge]['delay'] = delays[idx]

def _get_config_for_node(n):
    pathes = shortest_path(g, source=n)
    rules = []
    for idx, path in pathes.items():
        if idx==n: continue
        rules += {
            'dst_net': nodes[idx],
            'delay': sum([delays[k] for k in path]),
            'dispersion': '10ms'
        }
    return {
        'dev': 'eth0',
        'out_rate': '100kbps',
        'in_rate': '10kbps',
        'rules': rules
    }

def deploy():
    raise NotImplementedError()

def update():
    raise NotImplementedError()

def get_graph_json():
    """
    To be displayed in the frontend
    """
    raise NotImplementedError()    
    
