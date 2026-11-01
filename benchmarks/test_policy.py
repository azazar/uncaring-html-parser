import unittest
from policy import evaluate

class PolicyTest(unittest.TestCase):
    def test_identical_count_passes(self):
        statuses, regression, improvement = evaluate({'a': 100}, {'a': 100})
        self.assertEqual(statuses, {'a': 'PASS'})
        self.assertFalse(regression or improvement)

    def test_one_instruction_saved_requires_update(self):
        statuses, regression, improvement = evaluate({'a': 99}, {'a': 100})
        self.assertEqual(statuses['a'], 'UPDATE_REQUIRED')
        self.assertTrue(improvement)
        self.assertFalse(regression)

    def test_one_extra_instruction_fails(self):
        _, regression, improvement = evaluate({'a': 101}, {'a': 100})
        self.assertTrue(regression)
        self.assertFalse(improvement)

    def test_improvement_cannot_hide_regression(self):
        _, regression, improvement = evaluate({'a': 90, 'b': 101}, {'a': 100, 'b': 100})
        self.assertTrue(regression and improvement)

    def test_ratchet_preserves_gain(self):
        _, regression, _ = evaluate({'a': 100}, {'a': 99})
        self.assertTrue(regression)

    def test_scenario_changes_require_migration(self):
        with self.assertRaises(ValueError): evaluate({'b': 100}, {'a': 100})

    def test_missing_or_invalid_counter_fails(self):
        for value in (0, -1, 1.5, float('nan'), True):
            with self.assertRaises(ValueError): evaluate({'a': value}, {'a': 100})

if __name__ == '__main__': unittest.main()
