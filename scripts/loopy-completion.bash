#!/bin/bash
# Bash completion script for Loopy
# To enable: source this file or add to ~/.bashrc

_loopy() {
    local cur prev opts commands subcommands
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    
    # Main commands
    commands="run validate benchmark test-connection setup config tune report help"
    
    # Global options
    opts="--help -h --version -V --config -c --neo4j-uri -u --username -U --password -P --threads -t --duration -d --write-ratio -w --batch-size -b --node-labels -n --relationship-types -r --property-size --report-interval --csv-logging --csv-file --quiet -q --verbose -v"
    
    # Handle subcommands
    if [[ ${#COMP_WORDS[@]} -gt 2 ]]; then
        case "${COMP_WORDS[1]}" in
            config)
                local config_commands="init validate show edit"
                case "${COMP_WORDS[2]}" in
                    init|validate|show|edit)
                        case "${prev}" in
                            --config|-c|--output|-o)
                                _filedir
                                return 0
                                ;;
                        esac
                        ;;
                    *)
                        COMPREPLY=( $(compgen -W "${config_commands}" -- ${cur}) )
                        return 0
                        ;;
                esac
                ;;
            tune)
                case "${prev}" in
                    --profile|-p)
                        COMPREPLY=( $(compgen -W "light medium heavy stress" -- ${cur}) )
                        return 0
                        ;;
                    --config|-c)
                        _filedir
                        return 0
                        ;;
                esac
                local tune_opts="--profile -p --auto-tune --show-profiles --config -c --help -h"
                COMPREPLY=( $(compgen -W "${tune_opts}" -- ${cur}) )
                return 0
                ;;
            report)
                case "${prev}" in
                    --format|-f)
                        COMPREPLY=( $(compgen -W "html markdown csv" -- ${cur}) )
                        return 0
                        ;;
                    --template)
                        COMPREPLY=( $(compgen -W "basic detailed executive" -- ${cur}) )
                        return 0
                        ;;
                    --input|-i|--output|-o)
                        _filedir
                        return 0
                        ;;
                esac
                local report_opts="--input -i --output -o --format -f --template --help -h"
                COMPREPLY=( $(compgen -W "${report_opts}" -- ${cur}) )
                return 0
                ;;
            test-connection)
                case "${prev}" in
                    --save-report)
                        _filedir
                        return 0
                        ;;
                esac
                local test_opts="--neo4j-uri -u --nodes --username -U --password -P --full-diagnostics --diag --save-report --quick --help -h"
                COMPREPLY=( $(compgen -W "${test_opts}" -- ${cur}) )
                return 0
                ;;
            setup)
                case "${prev}" in
                    --output|-o)
                        _filedir
                        return 0
                        ;;
                esac
                local setup_opts="--output -o --skip-test --help -h"
                COMPREPLY=( $(compgen -W "${setup_opts}" -- ${cur}) )
                return 0
                ;;
        esac
    fi
    
    # Handle option values
    case "${prev}" in
        --neo4j-uri|-u)
            COMPREPLY=( $(compgen -W "bolt://localhost:7687 neo4j://localhost:7687" -- ${cur}) )
            return 0
            ;;
        --username|-U)
            COMPREPLY=( $(compgen -W "neo4j" -- ${cur}) )
            return 0
            ;;
        --config|-c|--csv-file)
            _filedir
            return 0
            ;;
        --write-ratio|-w)
            COMPREPLY=( $(compgen -W "0.1 0.3 0.5 0.7 0.9" -- ${cur}) )
            return 0
            ;;
        --threads|-t)
            COMPREPLY=( $(compgen -W "1 2 4 8 16" -- ${cur}) )
            return 0
            ;;
        --duration|-d)
            COMPREPLY=( $(compgen -W "30 60 120 300 600" -- ${cur}) )
            return 0
            ;;
        --batch-size|-b)
            COMPREPLY=( $(compgen -W "100 500 1000 2000" -- ${cur}) )
            return 0
            ;;
    esac
    
    # Complete commands and global options
    if [[ ${cur} == -* ]] ; then
        COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
    else
        COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    fi
    
    return 0
}

# Register the completion function
complete -F _loopy loopy

# Helpful aliases
alias loopy-quick='loopy run --duration 30 --threads 2'
alias loopy-test='loopy test-connection --quick'
alias loopy-setup='loopy setup'
alias loopy-config='loopy config show'

echo "Loopy bash completion loaded!"
echo "Available aliases: loopy-quick, loopy-test, loopy-setup, loopy-config"