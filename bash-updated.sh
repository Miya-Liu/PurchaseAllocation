#!/bin/bash

queries='1 6 8 9 10 11 12 13 15 18 24 25'
value=('l' 'm' 's' 'e')
alg=('3ddp' 'greedy' 'brute')
pas="PricingFunctionSettings/"
folder=('' 'CD1' 'CD2' 'CD3' 'CD4' 'CD5' 'CD6' 'CD7' 'LD1' 'LD2' 'LD3' 'LD4' 'LD5' 'LD6' 'LD7' 'LD8' 'LD9' 'LD10' 'LD11' 'LS1' 'LS2' 'LS3' 'LS4' 'LS5' 'LS6' 'LS7')

for index in $queries;
do
    for a in ${alg[@]};
    do
        for v in ${value[@]};
        do
            pfs=$pas${folder[$index]}
            for pf in $pfs/*;
            do
                java -jar serviceMarket.jar -pf ${pf} -v ${v} -i ${index} -a ${a}
            done
        done
    done    
done