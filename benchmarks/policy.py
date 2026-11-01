"""Exact instruction-count ratchet; no timing tolerances."""
def evaluate(actual, reference):
    if actual.keys() != reference.keys():
        raise ValueError('Scenarios changed: explicit baseline migration required.')
    if any(type(v) is not int or v <= 0 for v in [*actual.values(), *reference.values()]):
        raise ValueError('Instruction counts must be positive integers.')
    statuses = {name: ('REGRESSION' if count > reference[name] else
                       'UPDATE_REQUIRED' if count < reference[name] else 'PASS')
                for name, count in actual.items()}
    return statuses, 'REGRESSION' in statuses.values(), 'UPDATE_REQUIRED' in statuses.values()
