#!/bin/zsh
# Zsh completion script for Loopy
# To enable: source this file or add to ~/.zshrc

_loopy() {
    local context curcontext="$curcontext" state line
    typeset -A opt_args
    
    local commands=(
        'run:Execute load test (default behavior)'
        'validate:Validate configuration without running'
        'benchmark:Run predefined benchmark scenarios'
        'test-connection:Test Neo4j connectivity'
        'setup:Interactive setup wizard'
        'config:Configuration management commands'
        'tune:Performance tuning and optimization'
        'report:Generate detailed reports'
        'help:Show help information'
    )
    
    local global_opts=(
        '(-h --help)'{-h,--help}'[Show help information]'
        '(-V --version)'{-V,--version}'[Show version information]'
        '(-c --config)'{-c,--config}'[Configuration file path]:file:_files'
        '(-u --neo4j-uri)'{-u,--neo4j-uri}'[Neo4j connection URI]:uri:(bolt://localhost:7687 neo4j://localhost:7687)'
        '(-U --username)'{-U,--username}'[Neo4j username]:username:(neo4j)'
        '(-P --password)'{-P,--password}'[Neo4j password]:password:'
        '(-t --threads)'{-t,--threads}'[Number of worker threads]:threads:(1 2 4 8 16)'
        '(-d --duration)'{-d,--duration}'[Test duration in seconds]:seconds:(30 60 120 300 600)'
        '(-w --write-ratio)'{-w,--write-ratio}'[Write operation ratio]:ratio:(0.1 0.3 0.5 0.7 0.9)'
        '(-b --batch-size)'{-b,--batch-size}'[Batch size for operations]:size:(100 500 1000 2000)'
        '(-n --node-labels)'{-n,--node-labels}'[Comma-separated node labels]:labels:'
        '(-r --relationship-types)'{-r,--relationship-types}'[Comma-separated relationship types]:types:'
        '--property-size[Property size in bytes]:bytes:(50 100 500 1000)'
        '--report-interval[Statistics reporting interval]:interval:(5 10 30 60)'
        '--csv-logging[Enable CSV logging]:bool:(true false)'
        '--csv-file[CSV output file path]:file:_files'
        '(-q --quiet)'{-q,--quiet}'[Quiet mode - minimal output]'
        '(-v --verbose)'{-v,--verbose}'[Verbose mode - detailed output]'
    )
    
    _arguments -C \
        $global_opts \
        '1: :->commands' \
        '*::arg:->args' && return 0
    
    case $state in
        commands)
            _describe -t commands 'loopy commands' commands
            ;;
        args)
            case $line[1] in
                config)
                    local config_commands=(
                        'init:Generate default configuration'
                        'validate:Validate configuration file'
                        'show:Display current effective configuration'
                        'edit:Open configuration in default editor'
                    )
                    _arguments \
                        '1: :_describe "config commands" config_commands' \
                        '(-c --config)'{-c,--config}'[Configuration file]:file:_files' \
                        '(-o --output)'{-o,--output}'[Output file]:file:_files' \
                        '(-h --help)'{-h,--help}'[Show help]'
                    ;;
                tune)
                    _arguments \
                        '(-p --profile)'{-p,--profile}'[Load profile]:profile:(light medium heavy stress)' \
                        '--auto-tune[Apply automatic tuning recommendations]' \
                        '--show-profiles[Show all available load profiles]' \
                        '(-c --config)'{-c,--config}'[Configuration file to analyze]:file:_files' \
                        '(-h --help)'{-h,--help}'[Show help]'
                    ;;
                report)
                    _arguments \
                        '(-i --input)'{-i,--input}'[Input CSV file with test results]:file:_files' \
                        '(-o --output)'{-o,--output}'[Output file path]:file:_files' \
                        '(-f --format)'{-f,--format}'[Output format]:format:(html markdown csv)' \
                        '--template[Use predefined template]:template:(basic detailed executive)' \
                        '(-h --help)'{-h,--help}'[Show help]'
                    ;;
                test-connection)
                    _arguments \
                        '(-u --neo4j-uri)'{-u,--neo4j-uri}'[Neo4j connection URI]:uri:' \
                        '--nodes[Comma-separated list of cluster node URIs]:nodes:' \
                        '(-U --username)'{-U,--username}'[Neo4j username]:username:' \
                        '(-P --password)'{-P,--password}'[Neo4j password]:password:' \
                        '--full-diagnostics[Run comprehensive diagnostics]' \
                        '--diag[Run comprehensive diagnostics]' \
                        '--save-report[Save diagnostic report to file]:file:_files' \
                        '--quick[Quick connectivity test only]' \
                        '(-h --help)'{-h,--help}'[Show help]'
                    ;;
                setup)
                    _arguments \
                        '(-o --output)'{-o,--output}'[Output configuration file path]:file:_files' \
                        '--skip-test[Skip connection testing during setup]' \
                        '(-h --help)'{-h,--help}'[Show help]'
                    ;;
            esac
            ;;
    esac
}

# Register the completion function
compdef _loopy loopy

# Helpful aliases
alias loopy-quick='loopy run --duration 30 --threads 2'
alias loopy-test='loopy test-connection --quick'
alias loopy-setup='loopy setup'
alias loopy-config='loopy config show'

echo "Loopy zsh completion loaded!"
echo "Available aliases: loopy-quick, loopy-test, loopy-setup, loopy-config"