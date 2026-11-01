#!/usr/bin/env python3
"""Deterministic, thread-scoped Java bytecode counting with a JVMTI agent."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
from policy import evaluate

parser = argparse.ArgumentParser()
parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
parser.add_argument('--update-baseline', action='store_true')
parser.add_argument('--initialize-baseline', action='store_true', help='Create a missing baseline only.')
args = parser.parse_args()
if not args.java_home:
    parser.error('Set JAVA_HOME or --java-home to a pinned Linux JDK.')
root = Path(__file__).resolve().parents[1]
os.chdir(root)
jdk = Path(args.java_home).resolve()
java, javac = str(jdk / 'bin/java'), str(jdk / 'bin/javac')
repo = Path.home() / '.m2/repository/org/apache/commons'
jars = [repo / 'commons-lang3/3.18.0/commons-lang3-3.18.0.jar', repo / 'commons-text/1.10.0/commons-text-1.10.0.jar']
if not all(p.exists() for p in jars): parser.error('Run mvn test first.')
identity = {
    'jdk': subprocess.check_output([java, '--version'], text=True),
    'javac': subprocess.check_output([javac, '-version'], text=True),
    'sha256': {str(p.relative_to(root)) if p.is_relative_to(root) else p.name:
               hashlib.sha256(p.read_bytes()).hexdigest()
               for p in [root / 'benchmarks/InstructionBenchmark.java', root / 'benchmarks/instructions.c', *jars]},
    'vm_options': ['--enable-native-access=ALL-UNNAMED', '-Xint', '-Xms256m', '-Xmx256m', '-XX:+UseSerialGC', '-XX:ActiveProcessorCount=1'],
}
baseline_path = Path('benchmarks/instruction-baseline.json')
reference = json.loads(baseline_path.read_text()) if baseline_path.exists() else None
if args.initialize_baseline and reference is not None:
    parser.error('Baseline already exists; initialization cannot overwrite it.')
if reference is None and not args.initialize_baseline:
    parser.error('Missing baseline: explicitly use --initialize-baseline.')
if reference and reference['identity'] != identity:
    parser.error('JDK, dependencies or harness changed; explicit baseline migration required.')
cp = os.pathsep.join(map(str, jars))
with tempfile.TemporaryDirectory(prefix='parser-instructions-') as tmp:
    tmp = Path(tmp)
    library = tmp / 'instructions.so'
    subprocess.run(['cc', '-shared', '-fPIC', '-O2', '-Wall', '-Wextra', '-Werror',
                    '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
                    'benchmarks/instructions.c', '-o', str(library)], check=True)
    subprocess.run([javac, '--release', '11', '-cp', cp, '-d', str(tmp),
                    *map(str, sorted(Path('src/main/java').rglob('*.java'))),
                    'benchmarks/InstructionBenchmark.java'], check=True)
    runs = []
    for fork in range(3):
        print(f'Counting instructions in JVM {fork + 1}/3...', flush=True)
        output = subprocess.check_output([java, *identity['vm_options'],
                    '-agentpath:' + str(library), '-Dcounter.library=' + str(library),
                    '-cp', str(tmp) + os.pathsep + cp, 'InstructionBenchmark'], text=True)
        counts = {}
        for line in output.splitlines():
            name, count = line.split('\t')
            if name in counts: raise RuntimeError('Duplicate scenario: ' + name)
            counts[name] = int(count)
        if not counts: raise RuntimeError('No measurements returned.')
        runs.append(counts)
    if any(run != runs[0] for run in runs):
        raise RuntimeError('Counts differ between JVMs; baseline was not changed.')
actual = runs[0]
statuses, regression, improvement = evaluate(actual, reference['counts'] if reference else actual)
report = {'identity': identity, 'runs': runs, 'statuses': statuses}
Path('benchmarks/instruction-results.json').write_text(json.dumps(report, indent=2) + '\n')
for name, status in statuses.items(): print(f'{name}: {actual[name]} instructions; {status}')
if regression:
    print('REGRESSION: baseline update refused.')
    raise SystemExit(1)
if improvement and not args.update_baseline:
    print('UPDATE_REQUIRED: rerun with --update-baseline and commit the baseline.')
    raise SystemExit(2)
if reference is None or (improvement and args.update_baseline):
    temporary = baseline_path.with_suffix('.tmp')
    temporary.write_text(json.dumps({'identity': identity, 'counts': actual}, indent=2) + '\n')
    temporary.replace(baseline_path)
    print('Baseline saved. Commit instruction-baseline.json and instruction-results.json.')
